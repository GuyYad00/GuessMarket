package ui;

import engine.api.Engine;
import engine.api.EngineImpl;

public class Main {

    public static void main(String[] args) {
        // The single place in the system where the concrete engine class is created.
        Engine engine = new EngineImpl();
        new ConsoleUI(engine).run();
    }
}
