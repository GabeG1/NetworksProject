.SUFFIXES: .java .class
JVM_ARGS="-Dnashorn.args=--no-deprecation-warning"
.java.class:
	javac -nowarn $*.java

CLASSES = Server.java Client.java


default: classes

classes: $(CLASSES:.java=.class)

.PHONY : clean Server Client

Server: Server.class
	java Server

Client: Client.class
	java Client $(name)

clean:
	rm -f *.class