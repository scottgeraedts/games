rm *class
javac DominionServer.java
javac DominionClient.java
jar cfm Dominion.jar Manifest.txt *class DominionCards/*png
