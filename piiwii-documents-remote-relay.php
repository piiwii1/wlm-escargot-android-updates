<?php
/**
 * Plugin Name: PiiWii Documents Remote Relay
 * Description: Relais sécurisé sortant pour PiiWii Documents. Aucun accès entrant au PC.
 * Version: 0.1.0
 * Author: PiiWii
 */
if (!defined('ABSPATH')) { exit; }

const PIIWII_DOCS_REMOTE_NS = 'piiwii-documents-remote/v1';

function piiwii_docs_remote_hash_secret($secret) { return hash('sha256', (string)$secret); }
function piiwii_docs_remote_now() { return time(); }
function piiwii_docs_remote_devices() { $v=get_option('piiwii_docs_remote_devices',[]); return is_array($v)?$v:[]; }
function piiwii_docs_remote_save_devices($v) { update_option('piiwii_docs_remote_devices',$v,false); }
function piiwii_docs_remote_commands() { $v=get_option('piiwii_docs_remote_commands',[]); return is_array($v)?$v:[]; }
function piiwii_docs_remote_save_commands($v) { update_option('piiwii_docs_remote_commands',$v,false); }
function piiwii_docs_remote_results() { $v=get_option('piiwii_docs_remote_results',[]); return is_array($v)?$v:[]; }
function piiwii_docs_remote_save_results($v) { update_option('piiwii_docs_remote_results',$v,false); }

function piiwii_docs_remote_auth_device($req) {
    $body=$req->get_json_params();
    $id=sanitize_text_field($body['deviceId']??'');
    $secret=(string)($body['secret']??'');
    if(!$id || strlen($secret)<32) return false;
    $devices=piiwii_docs_remote_devices();
    if(empty($devices[$id]) || !empty($devices[$id]['revoked'])) return false;
    return hash_equals((string)$devices[$id]['secretHash'], piiwii_docs_remote_hash_secret($secret));
}

add_action('rest_api_init', function(){
    register_rest_route(PIIWII_DOCS_REMOTE_NS,'/register',[
        'methods'=>'POST','permission_callback'=>'__return_true','callback'=>function($req){
            $b=$req->get_json_params();
            $id=sanitize_text_field($b['deviceId']??''); $secret=(string)($b['secret']??'');
            $name=sanitize_text_field($b['name']??'PiiWii Documents');
            if(!preg_match('/^[a-zA-Z0-9_-]{16,80}$/',$id) || strlen($secret)<32) return new WP_Error('bad_request','Identifiants invalides',['status'=>400]);
            $devices=piiwii_docs_remote_devices();
            if(isset($devices[$id]) && !hash_equals((string)$devices[$id]['secretHash'],piiwii_docs_remote_hash_secret($secret))) return new WP_Error('conflict','Appareil déjà enregistré',['status'=>409]);
            $devices[$id]=['name'=>$name,'secretHash'=>piiwii_docs_remote_hash_secret($secret),'createdAt'=>$devices[$id]['createdAt']??piiwii_docs_remote_now(),'lastSeen'=>piiwii_docs_remote_now(),'version'=>sanitize_text_field($b['version']??''),'revoked'=>false];
            piiwii_docs_remote_save_devices($devices);
            return ['ok'=>true,'deviceId'=>$id,'pollSeconds'=>5];
        }
    ]);
    register_rest_route(PIIWII_DOCS_REMOTE_NS,'/poll',[
        'methods'=>'POST','permission_callback'=>'__return_true','callback'=>function($req){
            if(!piiwii_docs_remote_auth_device($req)) return new WP_Error('forbidden','Accès refusé',['status'=>403]);
            $b=$req->get_json_params(); $id=sanitize_text_field($b['deviceId']);
            $devices=piiwii_docs_remote_devices(); $devices[$id]['lastSeen']=piiwii_docs_remote_now(); piiwii_docs_remote_save_devices($devices);
            $all=piiwii_docs_remote_commands(); $now=piiwii_docs_remote_now(); $found=null;
            foreach($all as $k=>&$c){
                if(($c['deviceId']??'')!==$id) continue;
                if(($c['state']??'pending')==='done') continue;
                $lease=(int)($c['leaseUntil']??0);
                if(($c['state']??'pending')==='leased' && $lease>$now) continue;
                $c['state']='leased'; $c['leaseUntil']=$now+30; $found=$c; break;
            }
            unset($c); piiwii_docs_remote_save_commands($all);
            return ['ok'=>true,'command'=>$found];
        }
    ]);
    register_rest_route(PIIWII_DOCS_REMOTE_NS,'/result',[
        'methods'=>'POST','permission_callback'=>'__return_true','callback'=>function($req){
            if(!piiwii_docs_remote_auth_device($req)) return new WP_Error('forbidden','Accès refusé',['status'=>403]);
            $b=$req->get_json_params(); $id=sanitize_text_field($b['deviceId']); $cmd=sanitize_text_field($b['commandId']??'');
            if(!$cmd) return new WP_Error('bad_request','commandId manquant',['status'=>400]);
            $results=piiwii_docs_remote_results(); $results[$cmd]=['deviceId'=>$id,'createdAt'=>piiwii_docs_remote_now(),'ok'=>(bool)($b['ok']??false),'data'=>$b['data']??null,'error'=>sanitize_text_field($b['error']??'')];
            if(count($results)>100){ uasort($results,fn($a,$b)=>($a['createdAt']??0)<=>($b['createdAt']??0)); $results=array_slice($results,-100,null,true); }
            piiwii_docs_remote_save_results($results);
            $all=piiwii_docs_remote_commands(); foreach($all as &$c){ if(($c['id']??'')===$cmd){$c['state']='done';$c['doneAt']=piiwii_docs_remote_now();break;} } unset($c); piiwii_docs_remote_save_commands($all);
            return ['ok'=>true];
        }
    ]);
    register_rest_route(PIIWII_DOCS_REMOTE_NS,'/devices',[
        'methods'=>'GET','permission_callback'=>fn()=>current_user_can('manage_options'),'callback'=>function(){
            $out=[]; foreach(piiwii_docs_remote_devices() as $id=>$d){$out[]=['deviceId'=>$id,'name'=>$d['name']??'','version'=>$d['version']??'','lastSeen'=>$d['lastSeen']??0,'revoked'=>(bool)($d['revoked']??false)];} return ['devices'=>$out];
        }
    ]);
    register_rest_route(PIIWII_DOCS_REMOTE_NS,'/command',[
        'methods'=>'POST','permission_callback'=>fn()=>current_user_can('manage_options'),'callback'=>function($req){
            $b=$req->get_json_params(); $deviceId=sanitize_text_field($b['deviceId']??''); $type=sanitize_key($b['type']??'');
            $allowed=['ping','diagnostics','search','get_document','apply_classification'];
            if(!in_array($type,$allowed,true)) return new WP_Error('bad_type','Commande interdite',['status'=>400]);
            $devices=piiwii_docs_remote_devices(); if(empty($devices[$deviceId])||!empty($devices[$deviceId]['revoked'])) return new WP_Error('not_found','Appareil introuvable',['status'=>404]);
            $id=wp_generate_uuid4(); $all=piiwii_docs_remote_commands(); $all[]=['id'=>$id,'deviceId'=>$deviceId,'type'=>$type,'args'=>is_array($b['args']??null)?$b['args']:[],'state'=>'pending','createdAt'=>piiwii_docs_remote_now(),'leaseUntil'=>0];
            $cut=piiwii_docs_remote_now()-86400; $all=array_values(array_filter($all,fn($c)=>(int)($c['createdAt']??0)>$cut)); piiwii_docs_remote_save_commands($all);
            return ['ok'=>true,'commandId'=>$id];
        }
    ]);
    register_rest_route(PIIWII_DOCS_REMOTE_NS,'/result-admin',[
        'methods'=>'GET','permission_callback'=>fn()=>current_user_can('manage_options'),'callback'=>function($req){$id=sanitize_text_field($req->get_param('commandId')??'');$r=piiwii_docs_remote_results();return ['ready'=>isset($r[$id]),'result'=>$r[$id]??null];}
    ]);
    register_rest_route(PIIWII_DOCS_REMOTE_NS,'/revoke',[
        'methods'=>'POST','permission_callback'=>fn()=>current_user_can('manage_options'),'callback'=>function($req){$b=$req->get_json_params();$id=sanitize_text_field($b['deviceId']??'');$d=piiwii_docs_remote_devices();if(isset($d[$id])){$d[$id]['revoked']=true;piiwii_docs_remote_save_devices($d);}return ['ok'=>true];}
    ]);
});
