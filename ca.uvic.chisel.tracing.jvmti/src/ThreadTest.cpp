#include <boost/thread.hpp>
#include <ios>
#include <iostream>

#ifdef WIN32
//mingw and boost conflict with a link error on _tls_used. Defining this call
//back will get rid of the error. I'm not sure of the side-effects, though.
extern "C" void tss_cleanup_implemented() {}
#endif

static void thread_1() {
	using namespace std;
	int i = 0;
	for(i = 0; i < 100; i++) {
		cout << "Thread 1" << endl;
	}
}

static void thread_2() {
	using namespace std;
	for(int i = 0; i < 100; i++) {
		cout << "Thread 2" << endl;
	}
}

int main() {
	using namespace boost;
	thread t1(thread_1);
	thread t2(thread_2);
	t1.join();
	t2.join();
}
