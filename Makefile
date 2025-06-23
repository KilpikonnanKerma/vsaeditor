SRC_DIR = src
BIN_DIR = bin
MAIN_CLASS = Main.Main
JAR_FILE = $(BIN_DIR)/vsaEditor.jar

ifeq ($(OS),Windows_NT)
	SEP = ;
else
	SEP = :
endif

LIBS=lib/flatlaf-3.6.jar$(SEP)lib/json.jar

SOURCES := $(shell find $(SRC_DIR) -name "*.java")


all: clean $(BIN_DIR) build jar run

build:
	javac -cp "$(LIBS)" -d $(BIN_DIR) $(SOURCES)

$(BIN_DIR):
	mkdir -p $(BIN_DIR)

jar:
	echo "Main-Class: $(MAIN_CLASS)" > manifest.txt
	jar cfm $(JAR_FILE) manifest.txt -C $(BIN_DIR) .
	rm manifest.txt

run-jar: jar
	java -jar $(JAR_FILE)

run: all
	java -cp "$(BIN_DIR)$(SEP)$(LIBS)" $(MAIN_CLASS)

clean:
	rm -rf $(BIN_DIR)

.PHONY: all run clean