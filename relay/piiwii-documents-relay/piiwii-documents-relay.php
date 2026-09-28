<?php
/**
 * Plugin Name: PiiWii Documents Relay
 * Description: Relais sécurisé entre PiiWii Documents Windows et ChatGPT via WordPress.
 * Version: 0.1.0
 * Author: PiiWii
 */
if (!defined('ABSPATH')) exit;

final class PiiWii_Documents_Relay {
    const NS = 'piiwii-documents/v1';
    const OPT_DEVICES = 'piiwii_documents_relay_devices';
    const OPT_COMMANDS = 'piiwii_documents_relay_commands';
    public static function init(){ add_action('rest_api_init',[__CLASS__,'routes']); }
    public static function routes(){
        register_rest_route(self::NS,'/health',['methods'=>'GET','callback'=>fn()=>new WP_REST_Response(['ok'=>true,'version'=>'0.1.0'],200),'permission_callback'=>'__return_true']);
        register_rest_route(self::NS,'/device/register',['methods'=>'POST','callback'=>[__CLASS__,'device_register'],'permission_callback'=>'__return_true']);
        register_rest_route(self::NS,'/device/poll',['methods'=>'POST','callback'=>[__CLASS__,'device_poll'],'permission_callback'=>'__return_true']);
        register_rest_route(self::NS,'/device/result',['methods'=>'POST','callback'=>[__CLASS__,'device_result'],'permission_callback'=>'__return_true']);
        register_rest_route(self::NS,'/admin/devices',['methods'=>'GET','callback'=>[__CLASS__,'admin_devices'],'permission_callback'=>[__CLASS__,'can_admin']]);
        register_rest_route(self::NS,'/admin/command',['methods'=>'POST','callback'=>[__CLASS__,'admin_command'],'permission_callback'=>[__CLASS__,'can_admin']]);
        register_rest_route(self::NS,'/admin/status/(?P<id>[A-Za-z0-9_-]{8,80})',['methods'=>'GET','callback'=>[__CLASS__,'admin_status'],'permission_callback'=>[__CLASS__,'can_admin']]);
    }
    public static function can_admin(){ return current_user_can('manage_options'); }
    private static function body(WP_REST_Request $r){ $j=$r->get_json_params(); return is_array($j)?$j:[]; }
    private static function devices(){ $v=get_option(self::OPT_DEVICES,[]); return is_array($v)?$v:[]; }
    private static function save_devices($v){ update_option(self::OPT_DEVICES,$v,false); }
    private static function commands(){ $v=get_option(self::OPT_COMMANDS,[]); return is_array($v)?$v:[]; }
    private static function save_commands($v){ if(count($v)>250)$v=array_slice($v,-250,null,true); update_option(self::OPT_COMMANDS,$v,false); }
    private static function valid_device($id,$secret,&$device=null){
        if(!is_string($id)||!preg_match('/^[a-f0-9]{32}$/',$id))return false;
        if(!is_string($secret)||!preg_match('/^[a-f0-9]{64}$/',$secret))return false;
        $devices=self::devices(); if(!isset($devices[$id]))return false; $d=$devices[$id];
        if(!isset($d['secret_hash'])||!hash_equals($d['secret_hash'],hash('sha256',$secret)))return false; $device=$d; return true;
    }
    public static function device_register(WP_REST_Request $r){
        $b=self::body($r); $id=strtolower(trim((string)($b['device_id']??''))); $secret=strtolower(trim((string)($b['secret']??'')));
        if(!preg_match('/^[a-f0-9]{32}$/',$id)||!preg_match('/^[a-f0-9]{64}$/',$secret))return new WP_Error('bad_device','Identité appareil invalide',['status'=>400]);
        $devices=self::devices();
        if(isset($devices[$id])&&!hash_equals($devices[$id]['secret_hash']??'',hash('sha256',$secret)))return new WP_Error('device_conflict','Appareil déjà enregistré avec un autre secret',['status'=>409]);
        $devices[$id]=['secret_hash'=>hash('sha256',$secret),'label'=>sanitize_text_field($b['label']??''),'version'=>sanitize_text_field($b['version']??''),'last_seen'=>time(),'ip_hash'=>hash('sha256',(string)($_SERVER['REMOTE_ADDR']??''))];
        self::save_devices($devices); return ['ok'=>true];
    }
    public static function device_poll(WP_REST_Request $r){
        $b=self::body($r); $d=null; $id=strtolower(trim((string)($b['device_id']??''))); $secret=strtolower(trim((string)($b['secret']??'')));
        if(!self::valid_device($id,$secret,$d))return new WP_Error('forbidden','Appareil non autorisé',['status'=>403]);
        $devices=self::devices(); $devices[$id]['last_seen']=time(); self::save_devices($devices);
        $now=time(); $out=[]; $cmds=self::commands();
        foreach($cmds as $cid=>&$c){ if(($c['device_id']??'')!==$id||($c['status']??'')!=='queued')continue; if(($c['expires_at']??0)<$now){$c['status']='expired';continue;} $c['status']='delivered';$c['delivered_at']=$now;$out[]=['id'=>$cid,'action'=>$c['action'],'args'=>$c['args']??(object)[]];if(count($out)>=10)break; }
        unset($c); self::save_commands($cmds); return ['commands'=>$out];
    }
    public static function device_result(WP_REST_Request $r){
        $b=self::body($r); $d=null; $id=strtolower(trim((string)($b['device_id']??''))); $secret=strtolower(trim((string)($b['secret']??'')));
        if(!self::valid_device($id,$secret,$d))return new WP_Error('forbidden','Appareil non autorisé',['status'=>403]);
        $cid=sanitize_key($b['command_id']??''); $cmds=self::commands();
        if(!isset($cmds[$cid])||($cmds[$cid]['device_id']??'')!==$id)return new WP_Error('not_found','Commande introuvable',['status'=>404]);
        $cmds[$cid]['status']=!empty($b['ok'])?'done':'error'; $cmds[$cid]['result']=$b['result']??null; $cmds[$cid]['error']=sanitize_text_field((string)($b['error']??'')); $cmds[$cid]['finished_at']=time(); self::save_commands($cmds); return ['ok'=>true];
    }
    public static function admin_devices(){
        $out=[]; foreach(self::devices() as $id=>$d)$out[]=['device_id'=>$id,'label'=>$d['label']??'','version'=>$d['version']??'','last_seen'=>$d['last_seen']??0,'online'=>(time()-(int)($d['last_seen']??0))<30]; return ['devices'=>$out];
    }
    public static function admin_command(WP_REST_Request $r){
        $b=self::body($r); $device=sanitize_text_field($b['device_id']??''); $action=sanitize_key($b['action']??''); $allowed=['health','search','get_document','update_document'];
        if(!in_array($action,$allowed,true))return new WP_Error('not_allowed','Action non autorisée',['status'=>400]);
        $devices=self::devices(); if(!isset($devices[$device]))return new WP_Error('unknown_device','Appareil inconnu',['status'=>404]);
        $args=isset($b['args'])&&is_array($b['args'])?$b['args']:[]; $id=wp_generate_password(24,false,false); $cmds=self::commands(); $cmds[$id]=['device_id'=>$device,'action'=>$action,'args'=>$args,'status'=>'queued','created_at'=>time(),'expires_at'=>time()+300]; self::save_commands($cmds); return ['ok'=>true,'command_id'=>$id,'expires_at'=>time()+300];
    }
    public static function admin_status(WP_REST_Request $r){
        $id=sanitize_key($r['id']); $cmds=self::commands(); if(!isset($cmds[$id]))return new WP_Error('not_found','Commande introuvable',['status'=>404]); $c=$cmds[$id];
        return ['command_id'=>$id,'device_id'=>$c['device_id'],'action'=>$c['action'],'status'=>$c['status'],'result'=>$c['result']??null,'error'=>$c['error']??null,'created_at'=>$c['created_at']??null,'delivered_at'=>$c['delivered_at']??null,'finished_at'=>$c['finished_at']??null];
    }
}
PiiWii_Documents_Relay::init();
