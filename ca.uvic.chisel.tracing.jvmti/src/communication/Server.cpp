/*******************************************************************************
 * Copyright (c) 2009 the CHISEL group and contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	David Oulette - initial C implementation
 *     Del Myers - C++ implementation
 *******************************************************************************/
/*
 * Server.cpp
 *
 *  Created on: 18-Feb-2009
 *      Author: Del Myers
 */

#define BOOST_THREAD_USE_LIB

#include "communication/Server.hpp"



using namespace std;
//static oasis::Server* server = NULL;
static void stupid_runner() {
	for (int i = 0; i < 100; i++) {
		cout << "just testing the stupid threading! on server" << std::endl;
	}
}

namespace oasis {

Server::Server(VServerCallbacks* callbacks) :
	callbacks(callbacks),
	service(),
	socket(service),
	port(DEFAULT_PORT),
	client_open(false),
	client_connected(false){
	//stream.exceptions(tcp::iostream::eofbit | tcp::iostream::failbit | tcp::iostream::badbit);
}

void Server::ReceiveThread() {
	/* Receive loop */
	DEBUG_PRINT("Receive thread going");
	callbacks->InitReceiveThread();
	DEBUG_PRINT("Receive thread initialised");
//	while (client_open) {
//		DEBUG_PRINT("Receive thread waiting for command");
//		try {
//			CommandPtr cmd;
//
//			cmd = ReceiveCommand();
//
//			if (cmd == NULL) {
//				std::cerr << "n!";
//				boost::this_thread::sleep(boost::posix_time::seconds(1));
//			} else if (client_open && cmd && cmd->GetCommand() != ERROR_COMMAND) {
//				if(callbacks != NULL) {
//					callbacks->CommandReceived(*(cmd.get()));
//					//send a response
//					array_8 data;
//					data = array_8(new __int8[1]);
//					data[0] = cmd->GetCommand();
//					Command reply(ACK_COMMAND, 1, data);
//					SendCommand(reply);
//				} else {
//					client_open = false;
//				}
//			}
//		} catch (ios_base::failure &error) {
//			std::cerr << "Error reading from server thread: " << error.what() << std::endl;
//			//quits
//			client_open = false;
//		}
//	}
	if (client_open) {
		boost::asio::async_read(socket,
				boost::asio::buffer(read_command_header, 3),
				boost::bind(&Server::AsyncReceiveCommandHeader, this,
						boost::asio::placeholders::error
				)
		);
		service.run();
	}
//	callbacks->EndReceiveThread();

}

Server::Server(int port_num, VServerCallbacks* callbacks) :
	callbacks(callbacks),
	service(),
	socket(service),
	port(port_num),
	client_open(false),
	client_connected(false){
	//boost::ip::tcp::socket
	//stream.exceptions(tcp::iostream::eofbit | tcp::iostream::failbit | tcp::iostream::badbit);
}

Server::~Server() {
	DoQuit();
	receive_thread.reset();
}

void Server::Connect() {
	DEBUG_PRINT("Connecting Oasis Server");
	using boost::system::error_code;
	tcp::endpoint endpoint(tcp::v4(), GetPort());
	tcp::acceptor acceptor(service, endpoint);
	DEBUG_PRINT("Waiting for connection on " << GetPort());
	acceptor.accept(socket);
	DEBUG_PRINT("Connection received");
	//TODO: Check that the remote is localhost
	//	tcp::endpoint remote = stream.rdbuf()->remote_endpoint();
	//	boost::asio::ip::address addr = remote.address();
	Handshake();
	client_connected = true;
}

void Server::Start() {
	using namespace boost;
	//DEBUG_PRINT("Starting Oasis Server... attaching to receive thread at 0x" << std::hex << (int)(&Server::ReceiveThread));
	if (!client_connected) {
		std::cerr << "Attempted to start server before a connection was made";
		exit(1);
	}
	//start the receive thread
	try {
		//server = this;
		DEBUG_PRINT("Thread runner at " << std::hex << (intptr_t)(&stupid_runner));
		//thread t(stupid_runner);
		//receive_thread = shared_ptr<thread>(new thread(bind(&Server::ReceiveThread, this)));
		thread t(bind(&Server::ReceiveThread, this));
		//stupid_runner();
	} catch (std::exception& ex) {
		std::cerr << "Could not start receive thread: " << ex.what() << std::endl;
		exit(1);
	}

	//receive_thread = shared_ptr<thread>(new thread(stupid_runner));

}

void Server::Quit() {
	//service.post(boost::bind(&Server::DoQuit, this));
	DoQuit();
}

void Server::DoQuit() {
	client_open = false;
	socket.close();
}

int Server::GetPort() {
	return this->port;
}
/**
 * Sends and receives a short series of commands for setting up the protocol.
 */
void Server::Handshake() throw(ServerException){
	CommandPtr cmd;
	array_8 data;
	cout << "Waiting for connect command" << endl;
	try {
		cmd = ReceiveCommand();
		while (!cmd) {
			std::cerr << "nothing" << std::endl;
			boost::this_thread::sleep(boost::posix_time::milliseconds(200));
			cmd = ReceiveCommand();
		}
		if (cmd->GetCommand() != oasis::CONNECT_COMMAND) {
			string message = string("Expected ") + oasis::CONNECT_COMMAND + " and got " + cmd->GetCommand();
			throw ServerException::ProtocolError();
		}
		cout << "Got connect command" << endl;
		data = array_8(new __int8[1]);
		data.get()[0] = oasis::CONNECT_COMMAND;
		cmd = CommandPtr(new Command(ACK_COMMAND, 1, data));
		SendCommand(*cmd.get());

		cmd = ReceiveCommand();
		while (cmd->GetCommand() == FILTER_COMMAND) {
			//work with the filter commands
			__uint16 length = cmd->GetLength();
			if (length <= 0) {
				throw ServerException::ProtocolError();
			}
			//let the command handler know about the command
			callbacks->CommandReceived(*(cmd.get()));
			data.get()[0] = cmd->GetCommand();
			cmd = CommandPtr(new Command(ACK_COMMAND, 1, data));
			SendCommand(*cmd.get());
			cmd = ReceiveCommand();
		}
		while (!cmd) {
			std::cerr << "nothing" << std::endl;
			boost::this_thread::sleep(boost::posix_time::milliseconds(200));
			cmd = ReceiveCommand();
		}
		if (cmd->GetCommand() == START_COMMAND || cmd->GetCommand() == FILE_COMMAND) {
			__uint16 length = cmd->GetLength();
			if (length <= 0) {
				throw ServerException::ProtocolError();
			}
			//let the command handler know about the command
			callbacks->CommandReceived(*(cmd.get()));
			data.get()[0] = cmd->GetCommand();
			cmd = CommandPtr(new Command(ACK_COMMAND, 1, data));
			SendCommand(*cmd.get());
		} else {
			throw ServerException::ProtocolError();
		}
		client_open = true;
	} catch (ios_base::failure &e) {
		throw ServerException::IOError();
	} //note, ignore memory allocation; thread dies.
}



//void Server::SendThread() {
//	InitSendThread();
	/* currently does nothing */
//	EndSendThread();
//}

void Server::AsyncReceiveCommandHeader(const boost::system::error_code& error) {
	if (error) {
		return;
	}

	if (!client_open) {
		DoQuit();
		return;
	}

	//decode the message header.
	//recieve from the stream one byte.
	__int8 command = read_command_header[0];
	//get the next two bytes
	//set the length according to the two bytes
	//TODO: ensure correct byte order for different
	//platforms
	__uint16 length = ((read_command_header[1] & 0xFF) << 8) | (read_command_header[2] &0xFF);
	array_8 data(new char[length]);

	if (length > 0) {
		array_8 data(new char[length]);
		boost::asio::async_read(socket,
			boost::asio::buffer(data.get(), length),
			boost::bind(&Server::AsyncReceiveCommandData, this,
				boost::asio::placeholders::error,
				command, length, data
			)
		);
	} else {
		Command cmd(command, length, data);
		ProcessCommand(cmd);
		boost::asio::async_read(socket,
			boost::asio::buffer(read_command_header, 3),
			boost::bind(&Server::AsyncReceiveCommandHeader, this,
				boost::asio::placeholders::error
			)
		);
	}
}

void Server::AsyncReceiveCommandData(const boost::system::error_code& error, __int8 command, __uint16 size, array_8 data) {
	if (error) {
		DoQuit();
		return;
	}
	Command cmd(command, size, data);
	ProcessCommand(cmd);
	if (!client_open) {
		DoQuit();
		return;
	}
	//read the next one.
	boost::asio::async_read(socket,
		boost::asio::buffer(read_command_header, 3),
		boost::bind(&Server::AsyncReceiveCommandHeader, this,
			boost::asio::placeholders::error
		)
	);

}

void Server::ProcessCommand(Command& cmd) {
	//send the command to the callback, and then fire a response over the wire.
	callbacks->CommandReceived(cmd);
	//send a response
	array_8 data;
	data = array_8(new __int8[1]);
	data[0] = cmd.GetCommand();
	Command reply(ACK_COMMAND, 1, data);
	SendCommand(reply);
}

/**
 * Receives a single command from the stream, and blocks the current thread.
 * @return a shared pointer to the newly created command.
 * @throw ios_base::failure if the local stream could not be read
 * @throw bad_alloc if memory could not be allocated
 */
CommandPtr Server::ReceiveCommand() {
	__int8 command;
	__uint16 length;
	array_8 data;
//	//recieve from the stream one byte.
//	stream.get(command);
//
//	//get the next two bytes
//	char bytes[2];
//	stream.read(bytes, 2);
//
//	//set the length according to the two bytes
//	//TODO: ensure correct byte order for different
//	//platforms
//	length = ((bytes[0] & 0xFF) << 8) | (bytes[1] &0xFF);
//
//	//allocate memory for the data
//	data = array_8(new __int8[length]);
//
//	//get the data from the stream
//	stream.read(data.get(), length);

	__int8 header[3];
	socket.read_some(boost::asio::buffer(header, 3));
	command = header[0];
	length = ((header[1] & 0xFF) << 8) | (header[2] &0xFF);

	data = array_8(new __int8[length]);
	socket.read_some(boost::asio::buffer(data.get(), length));
	CommandPtr cmd(new Command(command, length, data));
	return cmd;
}

/**
 * Sends the single command through the stream.
 * @param command the command to send
 * @throw ios_base::failure if the stream could not be
 * written to.
 */
void Server::SendCommand(Command &command) {
	//send a chunk at a time

//	stream.put(command.GetCommand());
//	//the two bytes to send for the length
//	char bytes[2];
//	bytes[0] = (command.GetLength() & 0xFF00) >> 8;
//	bytes[1] = (command.GetLength() & 0xFF);
//	stream.write(bytes, 2);
//
//	//finally, send the data
//	stream.write(command.GetData().get(), command.GetLength());
//	stream.flush();

	__int8 header[3] = {
		command.GetCommand(),
		(command.GetLength() & 0xFF00) >> 8,
		(command.GetLength() & 0xFF)
	};
	socket.write_some(boost::asio::buffer(header, 3));
	socket.write_some(boost::asio::buffer(command.GetData().get(), command.GetLength()));

}

}
