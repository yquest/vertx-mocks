package pt.fabm;


public class Main {

    public static void main(String[] args) {
        DaggerApp.create().run().accept(args[0]);
    }
}