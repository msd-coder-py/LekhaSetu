LekhaSetu — Java backend connected to Stitch frontend

1. Open a terminal in backend.
2. Compile:
   javac -encoding UTF-8 -d out inventory/*.java
3. Run:
   java -cp out inventory.ApiServer
4. Open frontend/index.html in a browser.

The frontend now reads and writes inventory data through http://localhost:8080.
The Java Inventory object remains the persistent source of truth.
