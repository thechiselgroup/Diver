/*
 * Server.h
 *
 *  Created on: 18-Feb-2009
 *      Author: Del Myers
 */

#ifndef SERVER_H_
#define SERVER_H_
#include "stdafx.h"

#include <boost/asio.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/bind.hpp>
#include <boost/function.hpp>

#include <boost/date_time/posix_time/posix_time.hpp>
#include <iostream>


#include "communication/Command.hpp"

#include <windows.h>

#include <boost/thread.hpp>


using namespace boost::asio::ip;

namespace oasis {
const int DEFAULT_PORT = 27015;
/**
 * A class for the exceptions that may be thrown
 * from within the server.
 */
class ServerException : public std::exception {
private:
	int error;
public:
	/**
	 * Note passing a string is generally bad practice, but as we expect
	 * the errors to terminate the program anyway, it doesn't worry us.
	 * @param error_code
	 * @param error_message
	 * @return
	 */
	ServerException(int error_code) :
		error(error_code) {}
	virtual const char* what() const throw() {
		return "OASIS Server Error";
	};
	virtual ~ServerException() throw() {}

	static ServerException ProtocolError() {
		return ServerException(1);
	}
	static ServerException IOError() {
		return ServerException(2);
	}

	int GetError() {return error;}
};

/**
 * Callbacks used by the server. Pure virtual class
 * to be implemented by the class responsible for
 * callbacks.
 */
class VServerCallbacks {
public:
	VServerCallbacks() {}
	virtual ~VServerCallbacks() {}
	virtual void InitReceiveThread() = 0;
	virtual void EndReceiveThread() = 0;
	virtual void InitSendThread() = 0;
	virtual void EndSendThread() = 0;
	/**
	 * Indicates that the server has received a command
	 * that should be handled. The command types are
	 * described in Command.h
	 * @param cmd
	 */
	virtual void CommandReceived(Command &cmd) = 0;
};

/**
 * A small server that accepts a single client for communication. The client will
 * control when the tracing application pauses, starts, etc.
 */
class Server {
public:
	Server(VServerCallbacks* callbacks);
	Server(int port, VServerCallbacks* callbacks);
	/**
	 * Connects the server with the client. Must be called before
	 * the virtual machine starts so that communication can be
	 * made between the two before the VM attempts to perform any
	 * more actions.
	 */
	void Connect();
	/**
	 * Starts the send and receive threads of the server. Cannot be called
	 * before a connection is made.
	 */
	void Start();
	void Quit();
	int GetPort();
	virtual ~Server();
	void ReceiveThread();
protected:




	//void SendThread();

	VServerCallbacks* callbacks;

	boost::asio::io_service service;
	tcp::socket socket;
	//tcp::iostream stream;
	int port;
	volatile bool client_open;
	volatile bool client_connected;

	/**
	 * The state location for this server. Set by
	 * the client during the handshaking process.
	 */
	std::string file_location;

	/**
	 * Three bytes that represent the header of the next in-coming command
	 */
	__int8 read_command_header[3];

	boost::shared_ptr<boost::thread> receive_thread;

private:
	/**
	 * Sends and receives a short series of messages to set up the protocol.
	 */
	void Handshake() throw (ServerException);
	CommandPtr ReceiveCommand();
	void AsyncReceiveCommandHeader(const boost::system::error_code&);
	void AsyncReceiveCommandData(const boost::system::error_code& error, __int8 command, __uint16 size, array_8 data);
	void ProcessCommand(Command& cmd);
	void DoQuit();
	void SendCommand(Command &);
};



}

#endif /* SERVER_H_ */
