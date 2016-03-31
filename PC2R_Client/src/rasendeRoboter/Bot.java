package rasendeRoboter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

public class Bot {
	private static String[] names = {"garen", "mf", "jax", "lux", "darius", "lucian"};
	@SuppressWarnings("unused")
	private String userName, host;
	private Socket socket;
	private BufferedReader in;
	private PrintStream out;
	private Receive receiver;
	private Random rand;

	private int lastEnchere = Integer.MAX_VALUE;
	private int monEnchere = 0;
	private boolean firstMessage = true;
	private String lastActif;


	private Phase phase = null;
	private boolean tuEnchere = false;
	private boolean attenteStatutSolution = false;


	public Bot() {
		rand = new Random(System.currentTimeMillis()); 
		userName = names[rand.nextInt(names.length)];
		System.out.println("Nom d'utilisateur : "+userName);
		socket = connexion(userName, "localhost");

		// socket = connexion(userName, args[1]);
	}

	public Socket connexion(String username, String host) {

		if (Outils.checkHost(host)) {
			this.host= host;
			this.userName = username;
			try {
				if (socket == null) {
					socket = new Socket(host, Protocole.PORT);
					this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					this.out = new PrintStream(socket.getOutputStream(), true);
				}
				Protocole.connect(userName, out);
				String serverAnswer = in.readLine();
				if (serverAnswer.equals(Protocole.BIENVENUE+"/"+username+"/")) { 
					System.out.println(host+" a accepte connexion");
					this.receiver = new Receive(in);
					receiver.start();
				}
				else {
					System.out.println(username+" deja utilise");
					return null;
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		System.out.println("host = "+host);
		return socket;
	}

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		Bot bot = new Bot();
	}

	public void decoderReponseServer(String reponse) {
		String commande = Outils.getCommandeName(reponse);
		String user, message, data;
		switch (commande) {
		case Protocole.BIENVENUE:
			System.out.println("BIENVENUE");
			break;
		case Protocole.CONNECTE:
			user = Outils.getFirstArg(reponse);
			System.out.println(user+" s'est connecte");
			break;
		case Protocole.DECONNEXION:
			user = Outils.getFirstArg(reponse);
			System.out.println(user+" s'est deconnecte");
			break;
		case Protocole.RECEIVE_CHAT:
			user = Outils.getFirstArg(reponse);
			message = Outils.getSecondArg(reponse);
			if (!user.equals(userName)) {
				System.out.println(user+" : "+message);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Protocole.sendChat(userName, "ok", out);
			}
			break;
		case Protocole.SESSION:
			data = Outils.getFirstArg(reponse);
			phase = Phase.ATTENTE_TOUR;
			System.out.println("Attente tour");
			if (firstMessage) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				String msg = BotData.BIENVENUE[rand.nextInt(BotData.BIENVENUE.length)]; 
				Protocole.sendChat(userName, msg, out);
			}
			break;
		case Protocole.VAINQUEUR:
			data = Outils.getFirstArg(reponse);
			System.out.println("Fin de la session");
			break;
		case Protocole.TOUR:
			data= Outils.getSecondArg(reponse);
			// TODO lancer timer
			if (phase == Phase.ATTENTE_TOUR) {
				phase = Phase.REFLEXION;
				System.out.println("Phase de reflexion");
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Protocole.sendChat(userName, BotData.MESSAGE_PROVOC, out);
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.IL_A_TROUVE:
			if (phase == Phase.REFLEXION) {
				phase = Phase.ENCHERE;
				user = Outils.getFirstArg(reponse);
				data = Outils.getSecondArg(reponse);
				System.out.println(user+" a trouve une solution en "+data+" coups");
				System.out.println("Fin de la phase de reflexion");
				lastEnchere = Integer.parseInt(data);
				if (rand.nextDouble() < 0.10) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Protocole.sendChat(userName, "autant de coups "+user+" ?", out);
				}
				double alea = rand.nextDouble();
				System.out.println("proba enchere "+alea);
				if (alea < 0.7) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int enchere = lastEnchere - rand.nextInt(5) - 2;
					if (enchere > 0) {
						tuEnchere = true;
						monEnchere = enchere;
						Protocole.sendEnchere(userName, ""+enchere, out);
					}
				}
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.FIN_REFLEXION:
			if (phase == Phase.REFLEXION) {
				phase = Phase.ENCHERE;		
				lastEnchere = Integer.MAX_VALUE;
				double alea = rand.nextDouble();
				System.out.println("proba enchere "+alea);
				if (alea < 0.95) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					tuEnchere = true;
					Protocole.sendEnchere(userName, ""+53, out);
					monEnchere = 53;
					lastEnchere = 53;
				}
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.VALIDATION:
			if (phase == Phase.ENCHERE && tuEnchere) {
				System.out.println("Enchere validee");
				tuEnchere = false;				
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.ECHEC:
			if (phase == Phase.ENCHERE && tuEnchere) {
				user = Outils.getFirstArg(reponse);
				System.out.println("Enchere annulee car incoherente avec celle de "+user);
				tuEnchere = false;				
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.NOUVELLE_ENCHERE:
			if (phase == Phase.ENCHERE) {
				user = Outils.getFirstArg(reponse);
				data = Outils.getSecondArg(reponse);
				System.out.println(user+" a encheri avec "+data+" coups");
				lastEnchere = Integer.parseInt(data);
				double alea = rand.nextDouble();
				System.out.println("proba : "+alea+"  70%");
				if (alea < 0.7) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int enchere = lastEnchere - rand.nextInt(5)-2;
					System.out.println("lastEnchere = "+lastEnchere);
					if (enchere > 0) {
						tuEnchere = true;
						monEnchere = enchere;
						Protocole.sendEnchere(userName, ""+enchere, out);
					}
				}
				else {
					if (rand.nextDouble() < 0.5)
						Protocole.sendChat(userName, "pas mal "+user, out);
				}
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.FIN_ENCHERE:
			if (phase == Phase.ENCHERE) {
				phase = Phase.RESOLUTION;
				System.out.println("debut phase de resolution");
				user = Outils.getFirstArg(reponse);
				data = Outils.getSecondArg(reponse);
				System.out.println("Fin des encheres");
				if (!user.equals(userName)) {
					if (!user.equals("")) {
						System.out.println("Le joueur actif est "+user);
						lastActif = user;
					}
				}
				else {
					String sol = Outils.genererSolution(monEnchere);
					lastActif = userName;
					System.out.println("solution = "+sol);
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Protocole.sendSolution(userName, sol, out);
				}
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.SA_SOLUTION:
			if (phase == Phase.RESOLUTION && attenteStatutSolution == false) {
				user = Outils.getFirstArg(reponse);
				data = Outils.getSecondArg(reponse);
				if (!user.equals(userName)) {
					System.out.println(user+" a propose la solution suivante");
				}
				else {
					System.out.println("Vous avez propose la solution suivante");
				}
				attenteStatutSolution = true;
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.BONNE:
			if (phase == Phase.RESOLUTION && attenteStatutSolution) {
				if (lastActif.equals(userName)) {
					Protocole.sendChat(userName, "je ferai mieux la prochaine fois", out);
				}
				else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (rand.nextDouble() < 0.5)
						Protocole.sendChat(userName, "Bravo "+lastActif+" !", out);
					else
						Protocole.sendChat(userName, "Bien joue "+lastActif+" !", out);
				}
				System.out.println("Solution correcte");
				System.out.println("Fin du tour");
				lastActif = "";
				attenteStatutSolution = false;	
				phase = Phase.ATTENTE_TOUR;
				System.out.println("attente tour");
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.MAUVAISE:
			if (phase == Phase.RESOLUTION && attenteStatutSolution) {
				user = Outils.getFirstArg(reponse);
				System.out.println("Solution refusee");
				if (!lastActif.equals(userName)) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Protocole.sendChat(userName, "Dommage bien tente "+lastActif+" :)", out);
				}
				if (!user.equals(userName)) {
					System.out.println("Le joueur actif est "+user);
					System.out.println("Joueur Actif "+user);
					lastActif = user;
				}
				else {
					String sol = Outils.genererSolution(monEnchere);
					System.out.println("solution = "+sol);
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Protocole.sendSolution(userName, sol, out);
				}
				attenteStatutSolution = false;
			}
			else {
				System.err.println("Je ne dois pas passer ici");
			}
			break;
		case Protocole.FIN_RESOLUTION:
			if (phase == Phase.RESOLUTION) {
				System.out.println("Plus de joueurs restants");
				System.out.println("Fin du tour");
				phase = Phase.ATTENTE_TOUR;
				System.out.println("attente tour");
			}
			else {
				System.err.println("je ne dois pas passer ici");
			}
			break;
		case Protocole.TROP_LONG:
			if (phase == Phase.RESOLUTION) {
				user = Outils.getFirstArg(reponse);
				System.out.println("Temps depasse");
				if (!user.equals(userName)) {
					System.out.println("Le joueur actif est "+user);
				}
				else {
					String sol = Outils.genererSolution(monEnchere);
					System.out.println("solution = "+sol);
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Protocole.sendSolution(userName, sol, out);
				}
			}
			else {
				System.err.println(Protocole.TROP_LONG+" - je ne dois pas passer ici");
			}
			break;
		default:
			System.out.println("default "+reponse);
			break;
		}
	}


	class Receive extends Thread {
		private BufferedReader in;

		public Receive(BufferedReader in) {
			this.in = in;
		}

		@Override
		public void run() {
			String recu;
			try {
				while ((recu = in.readLine()) != null) {
					System.out.println("recu : "+recu);
					decoderReponseServer(recu);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}