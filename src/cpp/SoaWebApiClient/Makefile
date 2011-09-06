TARGET = soaclient
#all : soaclient

OBJS = main.o soaclient.o

CC = gcc

CFLAGS = -c -g


LIBS = -lcurl -lnsl -lssl -lcrypto -ljson

# Link the target with all objects and libraries
$(TARGET) : $(OBJS)
	$(CC)  -o $(TARGET) $(OBJS) $(LDFLAGS) $(LIBS)

# Compile the source files into object files
soaclient.o :  soaclient.c 
	$(CC) $(CFLAGS) $<

main.o : main.c 
	$(CC) $(CFLAGS) $<

clean:
	rm -rf *o soaclient
