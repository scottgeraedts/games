rm *class
javac -g -Xlint:unchecked DominionServer.java
javac -Xlint:unchecked DominionClient.java
jar cfm Dominion.jar Manifest.txt *class DominionCards/*jpg
