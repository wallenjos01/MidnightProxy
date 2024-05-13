#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>

#include <unistd.h>
#include <fcntl.h>
#include <pthread.h>
#include <signal.h>
#include <time.h>

int pipeFd = -1;
int childPid = -1;
int pipes[2];

const char* defaultPipePath = "/proxy/stdin";


void handleChildExit(int signal) {

	close(pipes[0]);
	close(pipes[1]);
	if(pipeFd != -1) {
		close(pipeFd);
	}
	exit(EXIT_SUCCESS);
}

void handleQuit(int signal) {

	char stopCmd[5] = { 's', 't', 'o', 'p', 10 };
	if(write(pipes[1], stopCmd, 5) == -1) {
		write(1, "Failed to stop gracefully!\n", 27);
		kill(childPid, SIGINT);
		sleep(10);
		exit(EXIT_FAILURE);
	}
	fsync(pipes[1]);
}

void main(int argc, const char** argv) {

	if(pipe(pipes) < 0) {
		printf("Unable to create pipes!");
		exit(1);
	}

	signal(SIGCHLD,handleChildExit);
	signal(SIGINT,handleQuit);

	childPid = fork();
	if(childPid == -1) { // Unable to fork
		exit(2);

	} else if(childPid == 0) { // Child Process

		dup2(pipes[0], 0); // Pipe input from main process to STDIN
		execlp("/proxy/start.sh", NULL);
		close(pipes[1]);
		close(pipes[0]);
		exit(EXIT_SUCCESS);

	} else { // Main Process

		char* pipePath = getenv("PROXY_STDIN_PATH");
		if(pipePath == NULL) {
			pipePath = defaultPipePath;
		}

		while(true) {

			pipeFd = open(pipePath, O_RDONLY);

			char buffer[512];
			size_t bytesRead = 0;
			while((bytesRead = read(pipeFd, buffer, 512)) > 0) {
				write(pipes[1], buffer, bytesRead);
			}

			close(pipeFd);
			pipeFd = -1;
		}
	}
}