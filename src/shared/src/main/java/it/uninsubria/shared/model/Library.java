package it.uninsubria.shared.model;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Library implements Serializable{
    private int id;
    private String name;
    private String user;
    private List<Book> libriDisponibili;
    private boolean isSelected;

    public Library(String name, List<Book> libriDisponibili){
        this.name = name;
        this.libriDisponibili = libriDisponibili;
    }

    public Library(int id, String user, String name){
        this.id = id;
        this.name = name;
    }
}
