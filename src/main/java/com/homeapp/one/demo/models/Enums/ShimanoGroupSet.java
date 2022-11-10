package com.homeapp.one.demo.models.Enums;

public enum ShimanoGroupSet {

    CLARIS("Claris"), SORA("Sora"), TIAGRA("Tiagra"), DURA_ACE("Dura Ace"),
    ONE_O_FIVE("105"), ULTEGRA("Ultegra");

    private String name;

    private ShimanoGroupSet(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ShimanoGroupSet fromName(String name) {
        return switch (name) {
            case "Claris" -> CLARIS;
            case "Sora" -> SORA;
            case "Tiagra" -> TIAGRA;
            case "105" -> ONE_O_FIVE;
            case "Ultegra" -> ULTEGRA;
            default -> DURA_ACE;
        };
    }

}