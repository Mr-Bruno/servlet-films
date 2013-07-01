TOMCAT_DIR = /home/clave/labs/lro/lro3/tomcat
APP_DIR = /home/clave/labs/lro/lro3/public_html/webapps/WEB-INF
SRC_DIR = $(APP_DIR)/src
TRGT_DIR = $(APP_DIR)/classes
SRVAPI_DIR = $(TOMCAT_DIR)/lib/servlet-api.jar
JFLAGS = -g -d $(TRGT_DIR) -sourcepath $(SRC_DIR) -classpath $(TRGT_DIR):$(SRVAPI_DIR)
JC = javac

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	$(SRC_DIR)/Consulta.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) -r $(TRGT_DIR)/*
