# 💬 Client-server ChatApp

A simple client-server chat application built entirely with **Java Swing**. It includes both a **GUI chat client** and a **multithreaded server**.

## ⚙️ Requirements
- Java **17** or newer (tested with Java 21 & 25)
- No external libraries required

## 🧱 How to Compile
Open a terminal or command prompt inside the project folder and run:
`javac -d bin src/*.java`
This will compile all `.java` files and place the `.class` files in the **bin** directory.

## 🚀 How to Create the JAR File
After compiling, run:
`jar cfe ChatApp.jar App -C bin .`
This will create a runnable JAR file named **ChatApp.jar** with `App` as the main class.

## 💻 How to Run
Run the application using:
`java -jar ChatApp.jar`
This automatically:
- Starts the chat server  
- Opens the chat client GUI  
You can also run components manually if you prefer:
java -cp bin ChatServer
java -cp bin ChatClientGUI
java -cp bin ChatClient

## 🧠 Notes
- Multiple clients can connect to the same server on `localhost:12345`.  
- To exit the chat, type `/exit` or close the client window.  

## 👨‍💻 Author
Made with ☕ by **Oussama Chikh**

