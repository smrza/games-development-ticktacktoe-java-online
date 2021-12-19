/*
 * #setMyName [name]
 * #start [withWhom]
 * #ai
 * #play [souřadnice X] [souřadnice Y]
 * #ia [souřadnice X] [souřadnice Y]
 */

package piskvorky;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

class ActiveHandlers {
	private static final long serialVersionUID = 1L;
	private HashSet<SocketHandler> activeHandlersSet = new HashSet<SocketHandler>();

	/**
	 * sendMessageToAll - Pošle zprávu všem aktivním klientùm kromì sebe sama
	 * 
	 * @param sender  - reference odesílatele
	 * @param message - øetìzec se zprávou
	 */
	synchronized void sendMessageToAll(SocketHandler sender, String message) {
		for (SocketHandler handler : activeHandlersSet) // pro všechny aktivní handlery
			if (handler != sender) {
				if (!handler.messages.offer(message)) // zkus pøidat zprávu do fronty jeho zpráv
					System.err.printf("Client %s message queue is full, dropping the message!\n", handler.clientID);
			}
	}

	/**
	 * add pøidá do množiny aktivních handlerù nový handler. Metoda je
	 * sychronizovaná, protože HashSet neumí multithreading.
	 * 
	 * @param handler - reference na handler, který se má pøidat.
	 * @return true if the set did not already contain the specified element.
	 */
	synchronized boolean add(SocketHandler handler) {
		return activeHandlersSet.add(handler);
	}

	/**
	 * remove odebere z množiny aktivních handlerù nový handler. Metoda je
	 * sychronizovaná, protože HashSet neumí multithreading.
	 * 
	 * @param handler - reference na handler, který se má odstranit
	 * @return true if the set did not already contain the specified element.
	 */
	synchronized boolean remove(SocketHandler handler) {
		return activeHandlersSet.remove(handler);
	}

	// start game [with player]
	synchronized void start(SocketHandler sender, String name) {
		sender.symbol = 'X';
		sender.playersTurn = sender.username;
		sender.playerVersus = name;

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				sender.board[i][j] = ' ';
			}
		}

		for (SocketHandler handler : activeHandlersSet) { // pro všechny aktivní handlery

			if (handler != sender && (name.compareTo(handler.username)==0)) {
				handler.symbol = 'O';
				handler.board = sender.board;
				handler.playersTurn = sender.username;
				handler.playerVersus = sender.username;
				

				if (!handler.messages.offer("\r\nSouper te vyzval ke hre. Hodne stesti!\r\nSouper je na tahu.")) { // zkus
																												// přidat
																												// zprávu
																												// do
					// fronty jeho zpráv
					System.err.printf("Client %s message queue is full, dropping the message!\n", handler.clientID);

				}
				if (!handler.messages.offer("\r\nHerni pole:" + drawBoard(handler.board))) // zkus přidat zprávu do
					System.err.printf("Client %s message queue is full, dropping the message!\n", handler.clientID);

			}

			if (handler == sender) {

				if (!handler.messages.offer("\r\nJsi na tahu.\r\nHerni pole:" + drawBoard(handler.board))) // zkus přidat
																										// zprávu do
					// fronty jeho zpráv
					System.err.printf("Client %s message queue is full, dropping the message!\n", handler.clientID);
			}

		}

	}
	
	// start game vs AI
		synchronized void startAI(SocketHandler sender) {
			sender.symbol = 'X';
			sender.aiSymbol = 'O';

			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					sender.board[i][j] = ' ';
				}
			}

			if (!sender.messages.offer("\r\nJsi na tahu.\r\nHerni pole:" + drawBoard(sender.board))) // zkus přidat zprávu do
				// fronty jeho zpráv
				System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
		}
		

	// play turn [x] [y]
	synchronized void play(SocketHandler sender, int Xaxis, int Yaxis) {

		if (sender.playersTurn.compareTo(sender.username) == 0) {
			if (sender.board[Xaxis][Yaxis] == ' ') {

				for (SocketHandler handler : activeHandlersSet) { // pro všechny aktivní handlery
					handler.board[Xaxis][Yaxis] = sender.symbol;

					if (handler != sender && (sender.playerVersus.compareTo(handler.username)==0)) {

						if (!handler.messages.offer("\r\nSouper odehral. Jste na tahu.")) // zkus přidat zprávu do fronty
																						// jeho zpráv
							System.err.printf("Client %s message queue is full, dropping the message!\n",
									handler.clientID);

						handler.playersTurn = handler.username;
						sender.playersTurn = handler.username;
					}

					if (handler == sender) {
						if (!handler.messages.offer("\r\nSouper je na tahu.")) // zkus přidat zprávu do fronty jeho zpráv
							System.err.printf("Client %s message queue is full, dropping the message!\n",
									handler.clientID);
					}

					if (!handler.messages.offer("\r\nHerni pole:" + drawBoard(handler.board))) { // zkus přidat zprávu do
						// fronty jeho zpráv
						System.err.printf("Client %s message queue is full, dropping the message!\n", handler.clientID);
					}

					if (victoryConditions(handler.board, sender.symbol, Xaxis, Yaxis)) {
						if (!handler.messages.offer("\r\nVitezi: " + sender.symbol + "\r\nDobra hra.")) // zkus přidat
																									// zprávu do fronty
																									// jeho zpráv
							System.err.printf("Client %s message queue is full, dropping the message!\n",
									handler.clientID);
					}
					if (drawConditions(handler.board)) {
						if (!handler.messages.offer("\r\nHra skoncila remizou.")) // zkus přidat zprávu do fronty jeho
																				// zpráv
							System.err.printf("Client %s message queue is full, dropping the message!\n",
									handler.clientID);
					}
				}
			} else {
				if (!sender.messages.offer("\r\nTohle pole je jiz zabrane, zvolte jine pole.")) // zkus přidat zprávu do
																								// fronty jeho zpráv
					System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
			}
		}

		else {
			if (!sender.messages.offer("\r\nVyckejte na svuj tah.")) // zkus přidat zprávu do fronty jeho zpráv
				System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
		}
	}
	
	
	
	// play turn [x] [y]
	synchronized void playAI(SocketHandler sender, int Xaxis, int Yaxis) {

		if (sender.board[Xaxis][Yaxis] == ' ') {

			sender.board[Xaxis][Yaxis] = sender.symbol;

			if (!sender.messages.offer("\r\nSouper je na tahu.")) // zkus přidat zprávu do fronty jeho zpráv
				System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);

			if (!sender.messages.offer("\r\nHerni pole:" + drawBoard(sender.board))) { // zkus přidat zprávu do
				// fronty jeho zpráv
				System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
			}

			Boolean aiPlayed = false;
			int x = 0;
			int y = 0;

			while (!aiPlayed) {
				x = new Random().nextInt(3);
				y = new Random().nextInt(3);

				if (sender.board[x][y] == ' ') {
					aiPlayed = true;
					sender.board[x][y] = sender.aiSymbol;
				}
			}
			
			if (!sender.messages.offer("\r\nPocitac odehral. Jsi na tahu.")) // zkus přidat zprávu do fronty jeho zpráv
				System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
			
			if (!sender.messages.offer("\r\nHerni pole:" + drawBoard(sender.board))) { // zkus přidat zprávu do
				// fronty jeho zpráv
				System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
			}
			

			if (victoryConditions(sender.board, sender.symbol, Xaxis, Yaxis)) {
				if (!sender.messages.offer("\r\nVitezi: " + sender.symbol + "\r\nDobra hra.")) // zkus přidat zprávu do
																							// fronty jeho zpráv
					System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
				return;
			}
			
			if (victoryConditions(sender.board, sender.aiSymbol, x, y)) {
				if (!sender.messages.offer("\r\nVitezi: " + sender.aiSymbol + "\r\nDobra hra.")) // zkus přidat zprávu do
																							// fronty jeho zpráv
					System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
				return;
			}
			
			if (drawConditions(sender.board)) {
				if (!sender.messages.offer("\r\nHra skoncila remizou.")) // zkus přidat zprávu do fronty jeho zpráv
					System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
			}

		} else {
			if (!sender.messages.offer("\r\nTohle pole je jiz zabrane, zvolte jine pole.")) // zkus přidat zprávu do
																							// fronty jeho zpráv
				System.err.printf("Client %s message queue is full, dropping the message!\n", sender.clientID);
		}

	}
	
	
	
	String drawBoard(char[][] board) {
		return "\r\n\r\n" + "    0   1   2    \r\n" + " 0  " + board[0][0] + " | " + board[0][1] + " | " + board[0][2] + " \r\n"
				+ "   ---|---|---\r\n" + " 1  " + board[1][0] + " | " + board[1][1] + " | " + board[1][2] + " \r\n"
				+ "   ---|---|---\r\n" + " 2  " + board[2][0] + " | " + board[2][1] + " | " + board[2][2] + " \r\n";
	}

	Boolean victoryConditions(char[][] board, char symbol, int x, int y) {
		int n = 3;
		// column
		for (int i = 0; i < n; i++) {
			if (board[x][i] != symbol)
				break;
			if (i == n - 1) {
				return true;
			}
		}

		// row
		for (int i = 0; i < n; i++) {
			if (board[i][y] != symbol)
				break;
			if (i == n - 1) {
				return true;
			}
		}

		
		// diagonal
		if(board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) {
			return true;
		}
		// anti diagonal
		if(board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol) {
			return true;
		}

		return false;
	}

	Boolean drawConditions(char[][] board) {
		Boolean flag = true;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (board[i][j] == ' ') {
					flag = false;
				}
			}
		}
		return flag;
	}
}



class SocketHandler {
	/** mySocket je socket, o který se bude tento SocketHandler starat */
	Socket mySocket;

	/** client ID je øetìzec ve formátu <IP_adresa>:<port> */
	String clientID;
	// string nickname for user
	String username;

	char[][] board = new char[3][3];
	// players assigned symbol
	char symbol;
	// determining whose turn it is
	String playersTurn;
	// symbol for ai
	char aiSymbol;
	//saves your current opponent
	String playerVersus = "";

	/**
	 * activeHandlers je reference na množinu všech právì bìžících SocketHandlerù.
	 * Potøebujeme si ji udržovat, abychom mohli zprávu od tohoto klienta poslat
	 * všem ostatním!
	 */
	ActiveHandlers activeHandlers;

	/**
	 * messages je fronta pøíchozích zpráv, kterou musí mít kažý klient svoji
	 * vlastní - pokud bude je pøetížená nebo nefunkèní klientova sí, èekají zprávy
	 * na doruèení právì ve frontì messages
	 */
	ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<String>(20);

	/**
	 * startSignal je synchronizaèní závora, která zaøizuje, aby oba tasky
	 * OutputHandler.run() a InputHandler.run() zaèaly ve stejný okamžik.
	 */
	CountDownLatch startSignal = new CountDownLatch(2);

	/** outputHandler.run() se bude starat o OutputStream mého socketu */
	OutputHandler outputHandler = new OutputHandler();
	/** inputHandler.run() se bude starat o InputStream mého socketu */
	InputHandler inputHandler = new InputHandler();
	/**
	 * protože v outputHandleru nedovedu detekovat uzavøení socketu, pomùže mi
	 * inputFinished
	 */
	volatile boolean inputFinished = false;

	public SocketHandler(Socket mySocket, ActiveHandlers activeHandlers) {
		this.mySocket = mySocket;
		clientID = mySocket.getInetAddress().toString() + ":" + mySocket.getPort();
		username = clientID;
		this.activeHandlers = activeHandlers;

		// for(int i = 0; i<3;i++) {
		// for(int j = 0; j<3;j++) {
		// board[i][j] = ' ';
		// }
		// }
	}

	// funkce slouží pro nastavení nickname
	public boolean setMyName(String nickname) {
		if (nickname.startsWith("#setMyName")) {
			// nastaví se nickname na celý vstup (#setMyName Name), a odebere se
			// "#setMyName"
			String str = nickname;
			str = str.replace("#setMyName ", "");
			username = str;
			return true;
		}
		return false;
	}

	class OutputHandler implements Runnable {
		public void run() {
			OutputStreamWriter writer;
			try {
				System.err.println("DBG>Output handler starting for " + clientID);
				startSignal.countDown();
				startSignal.await();
				System.err.println("DBG>Output handler running for " + clientID);
				writer = new OutputStreamWriter(mySocket.getOutputStream(), "UTF-8");
				writer.write("\nYou are connected from " + clientID + "\n");
				writer.flush();
				while (!inputFinished) {
					String m = messages.take();// blokující ètení - pokud není ve frontì zpráv nic, uspi se!
					writer.write(m + "\r\n"); // pokud nìjaké zprávy od ostatních máme,
					writer.flush(); // pošleme je našemu klientovi
					System.err.println("DBG>Message sent to " + clientID + ":" + m + "\n");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.err.println("DBG>Output handler for " + clientID + " has finished.");
		}
	}

	class InputHandler implements Runnable {
		public void run() {
			try {
				System.err.println("DBG>Input handler starting for " + clientID);
				startSignal.countDown();
				startSignal.await();
				System.err.println("DBG>Input handler running for " + clientID);
				String request = "";
				/**
				 * v okamžiku, kdy nás Thread pool spustí, pøidáme se do množiny všech aktivních
				 * handlerù, aby chodily zprávy od ostatních i nám
				 */
				activeHandlers.add(SocketHandler.this);
				BufferedReader reader = new BufferedReader(new InputStreamReader(mySocket.getInputStream(), "UTF-8"));
				while ((request = reader.readLine()) != null) { // pøišla od mého klienta nìjaká zpráva?

					//////////////////////////////////////////////////////////
					// když request začíná #setMyName
					if (setMyName(request)) {
					} else if (request.startsWith("#start")) {
						String[] requested = request.split(" ");
						String opponent = requested[1];
						activeHandlers.start(SocketHandler.this, opponent);
					} else if (request.startsWith("#play")) {
						String[] requested = request.split(" ");
						char[] Xaxis = requested[1].toCharArray();
						char[] Yaxis = requested[2].toCharArray();
						activeHandlers.play(SocketHandler.this, Character.getNumericValue(Xaxis[0]),
								Character.getNumericValue(Yaxis[0]));
					} else if (request.startsWith("#ai")) {
						activeHandlers.startAI(SocketHandler.this);
					} else if (request.startsWith("#ia")) {
						String[] requested = request.split(" ");
						char[] Xaxis = requested[1].toCharArray();
						char[] Yaxis = requested[2].toCharArray();
						activeHandlers.playAI(SocketHandler.this, Character.getNumericValue(Xaxis[0]),
								Character.getNumericValue(Yaxis[0]));
					} else {
						if (username != null) {
							request = "From " + username + ": " + request;
							System.out.println(request);
							activeHandlers.sendMessageToAll(SocketHandler.this, request);
						} else {
							// ano - pošli ji všem ostatním klientùm
							request = "From client " + clientID + ": " + request;
							System.out.println(request);
							activeHandlers.sendMessageToAll(SocketHandler.this, request);
						}
					}
				}

				inputFinished = true;
				messages.offer("OutputHandler, wakeup and die!");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				// remove yourself from the set of activeHandlers
				synchronized (activeHandlers) {
					activeHandlers.remove(SocketHandler.this);
				}
			}
			System.err.println("DBG>Input handler for " + clientID + " has finished.");
		}
	}
}

public class piskvorky {

	public static void main(String[] args) {
		int port = 33000, max_conn = 100;

		if (args.length > 0) {
			if (args[0].startsWith("--help")) {
				System.out.printf("Usage: Server [PORT] [MAX_CONNECTIONS]\n"
						+ "If PORT is not specified, default port %d is used\n"
						+ "If MAX_CONNECTIONS is not specified, default number=%d is used", port, max_conn);
				return;
			}
			try {
				port = Integer.decode(args[0]);
			} catch (NumberFormatException e) {
				System.err.printf("Argument %s is not integer, using default value", args[0], port);
			}
			if (args.length > 1)
				try {
					max_conn = Integer.decode(args[1]);
				} catch (NumberFormatException e) {
					System.err.printf("Argument %s is not integer, using default value", args[1], max_conn);
				}

		}
		// TODO Auto-generated method stub
		System.out.printf("IM server listening on port %d, maximum nr. of connections=%d...\n", port, max_conn);
		ExecutorService pool = Executors.newFixedThreadPool(2 * max_conn);
		ActiveHandlers activeHandlers = new ActiveHandlers();

		try {
			ServerSocket sSocket = new ServerSocket(port);
			do {
				Socket clientSocket = sSocket.accept();
				clientSocket.setKeepAlive(true);
				SocketHandler handler = new SocketHandler(clientSocket, activeHandlers);
				pool.execute(handler.inputHandler);
				pool.execute(handler.outputHandler);
			} while (!pool.isTerminated());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			pool.shutdown();
			try {
				// Wait a while for existing tasks to terminate
				if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
					pool.shutdownNow(); // Cancel currently executing tasks
					// Wait a while for tasks to respond to being cancelled
					if (!pool.awaitTermination(60, TimeUnit.SECONDS))
						System.err.println("Pool did not terminate");
				}
			} catch (InterruptedException ie) {
				// (Re-)Cancel if current thread also interrupted
				pool.shutdownNow();
				// Preserve interrupt status
				Thread.currentThread().interrupt();
			}
		}
	}
}
