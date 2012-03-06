/*
 * Command.h
 *
 *  Created on: 3-Mar-2009
 *      Author: Del Myers
 */

#ifndef COMMAND_H_
#define COMMAND_H_
#include "utils/standard_ptrs.hpp"
#include "integers.h"
namespace oasis {
/*
 * The "command acknowledged" command. Sent to clients after a command has been
 * received. The data of the command will always be the value of the command last
 * received. In case of an error (for example, the server is in the wrong state to
 * receive a command), an acknowledgement will not be sent; an error will be sent
 * instead.
 */
const __int8 ACK_COMMAND = 'a';
const __int16 ACK_COMMAND_LENGTH = 1;

/*
 * An error command. Sent by the server to the client to indicate that an error has
 * occurred. The data will be an ASCII __int8acter array with a length equal to
 * the size of data_len will always be 8, and the data will be an error code as
 * defined in oasis_errors.h.
 */
const __int8 ERROR_COMMAND = 'e';

/**
 * The "connect command" sent by clients to indicate that they are ready for a connection.
 * It is expected to be immediately followed by a FILE_COMMAND or a START_COMMAND.
 */
const __int8 CONNECT_COMMAND = 'c';
const __int16 CONNECT_COMMAND_LENGTH = 0;

/**
 * The command used to set a new file for logging. Sent by clients to the server.
 * It may come after a CONNECT_COMMAND
 * in order to set the file, but not begin tracing immediately. The data is expected
 * to be an ASCII character array of length data_len, containing the path to the
 * destination file. The character array is not expected to be null terminating, and
 * should not contain any control characters (such as new lines or line feeds).
 */
const __int8 FILE_COMMAND = 'f';

/**
 * The command used to start logging. Sent by clients to the server.
 * If data_len is zero, than a previous FILE_COMMAND
 * must have been sent to set the log file. Otherwise, the semantics for the data
 * are the same as FILE_COMMAND.
 */
const __int8 START_COMMAND = 's';

/**
 * The command to pause logging. Sent by clients to the server. data_len is expected
 * to be zero. Can only be called after a START_COMMAND, and has no meaning if already
 * paused. If either of these cases occurs, then clients will receive an error.
 */
const __int8 PAUSE_COMMAND = 'p';

/**
 * The command to resume previously paused logging. Sent by clients to the server.
 * data_len is expected to be zero. Can only be called while the server has paused
 * logging, otherwise an error will be issued.
 */
const __int8 RESUME_COMMAND = 'r';

/**
 * The command used to set a wild-card filter for class names in the tracer. The
 * first byte of the command indicates whether the class name should be included
 * or excluded from the trace (1 for include, 0 for exclude). The following
 * bytes represent a string indicating the wild-card matcher.
 */
const __int8 FILTER_COMMAND = 'x';

class Command {
public:
	Command(__int8 c, __uint16 length, array_8 data);
	__int8 GetCommand();
	__uint16 GetLength();
	array_8 GetData();
	virtual ~Command();
private:
	__int8 command;
	__uint16 length;
	array_8 data;
};
/**
 * For convenience, a shared pointer for commands that
 * can be passed around without worrying about
 * memory management.
 */
typedef boost::shared_ptr<Command> CommandPtr;
}


#endif /* COMMAND_H_ */
