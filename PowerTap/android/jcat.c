#include <sys/types.h>
#include <sys/select.h>
#include <errno.h>
#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>

#define true 1
#define BUFFER_SIZE 64*1024
int main(int argc, char **argv)
{
    int fd;
    int n;
    fd_set set;
    ssize_t bytes;
    size_t total_bytes;
    char buf[BUFFER_SIZE];


	if (argc!=2){
	  printf("usage: jcat filename\n");
	  exit(-1);
	}

    fd = open(argv[1], O_RDONLY | O_NONBLOCK);
    if (fd == -1) {
        perror("open");
        return EXIT_FAILURE;
    }

    FD_ZERO(&set);
    FD_SET(fd, &set);

    while (true) {
        n = select(fd+1, &set, NULL, NULL, NULL);
        if (!n)
            continue;
        if (n == -1) {
            perror("select");
            return EXIT_FAILURE;
        }
        if (FD_ISSET(fd, &set)) {
            total_bytes = 0;
            for (;;) {
                bytes = read(fd, buf, sizeof(buf)-1);
				buf[bytes]='\0';
				printf("%s", buf);
               if (bytes<1000){
                    if (errno == EWOULDBLOCK) {
						return 0;
                    } else {
                        return EXIT_FAILURE;
                    }
                }
            }
        }
    }

    return EXIT_SUCCESS;
}

