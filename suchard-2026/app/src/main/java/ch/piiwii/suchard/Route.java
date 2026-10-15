package ch.piiwii.suchard;
final class Route {
 static final class Stop {
   final String time,title,place,note,destination,mode,emoji;
   final int hour,minute; final boolean reminder;
   Stop(String t,String n,String p,String desc,String gps,String travel,String ico,int h,int m,boolean notify){
     time=t;title=n;place=p;note=desc;destination=gps;mode=travel;emoji=ico;hour=h;minute=m;reminder=notify;
   }
 }
 static final Stop[] ALL={
 new Stop("07:30","Départ de Sion","Direction Neuchâtel","Départ en voiture, prévoir une marge de circulation.","Parking du Seyon, Rue du Seyon 38, Neuchâtel, Suisse","driving","🚗",7,15,true),
 new Stop("09:30","Parking du Seyon","Rue du Seyon 38","Stationner puis partir visiter le centre à pied.","Parking du Seyon, Neuchâtel, Suisse","driving","🅿️",9,15,false),
 new Stop("09:50","Confiserie Wodey-Suchard","Grand-Rue 1","Découvrir la confiserie historique de Philippe Suchard et ses spécialités.","Grand-Rue 1, Neuchâtel, Suisse","walking","🍫",9,40,true),
 new Stop("10:20","Carac Rose géant","Temple du Bas","Animations et stands. Arriver avant 11 h.","Rue du Temple-Neuf 8, Neuchâtel, Suisse","walking","🩷",10,5,true),
 new Stop("11:00","Dévoilement du Carac Rose","Temple-Neuf 8","Présentation du carac géant, photo et partage.","Rue du Temple-Neuf 8, Neuchâtel, Suisse","walking","🎂",10,55,true),
 new Stop("11:30","Vieille ville et lac","Centre de Neuchâtel","Promenade, photos et panneaux historiques.","Quai Ostervald, Neuchâtel, Suisse","walking","📸",11,20,true),
 new Stop("12:15","Pause sandwichs","Bord du lac","Sandwichs emportés, sans restaurant.","Quai Ostervald, Neuchâtel, Suisse","walking","🥪",12,5,true),
 new Stop("13:15","Exposition et fresque","Fondation Alfaset, Tivoli 34","« Nous étions Suchard », gratuit 11 h–17 h. Fresque GEHOD. Prendre la voiture vers 12 h 50.","Rue de Tivoli 34, Neuchâtel, Suisse","driving","🖼️",12,50,true),
 new Stop("14:00","Usines Suchard et Sugus","Serrières","Pont Berthier, bâtiments historiques, recherche des Sugus géants. Adresse exacte non confirmée.","Rue Guillaume-Farel 24, Neuchâtel, Suisse","walking","🏭",13,45,true),
 new Stop("14:45","Jeu de piste Milka","Esplanade de Serrières","Totemi : à la recherche de la vache Milka, gratuit.","Tranchée couverte de Serrières, Neuchâtel, Suisse","walking","🐄",14,30,true),
 new Stop("16:15","Promenade au volant","Temps libre","Balade en voiture et photos au rythme de chacun. Visite facultative à votre rythme, sans Boudry.","","driving","🌅",16,0,false),
 new Stop("17:15","Retour à Sion","Neuchâtel → Sion","Route du retour. Arrivée estimée vers 19 h 15.","Sion, Suisse","driving","🏠",17,0,true)
 };
}
