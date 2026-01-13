---
mainfont: "DejaVu Sans"
monofont: "DejaVu Sans Mono"
---


# Diagramma ER


### 1. Full digramma ER
![ER_DIAGRAM](../immagini/ER_DIAGRAM.png)



## 2. Dettagli entità relazioni
![ER_DIAGRAM](../immagini/ER_DIAGRAM2.png)



## 4. Relazione Libreria-Libro 
![ER_DIAGRAM](../immagini/ER_DIAGRAM3.png)



## 5. Schema sistema di suggerimenti
![ER_DIAGRAM](../immagini/ER_DIAGRAM4.png)


## 7. Costanti e indici
### Relazioni chiavi esterne

![ER_DIAGRAM](../immagini/ER_DIAGRAM5.png)


### Vincoli di unicità

![ER_DIAGRAM](../immagini/ER_DIAGRAM5.png)





## Come sono stati creati i diagrammi? 
Per la realizzazione degli schemi è stato utilizzato **Mermaid**, software online che permette di caricare un codice con una determinata sintassi e viene tramutato in diagramma

### Generatori di diagrammi online
1. [Mermaid Live Editor](https://mermaid.live/) 
2. [Mermaid Chart](https://www.mermaidchart.com/) 
3. [Draw.io](https://app.diagrams.net/) 
4. [PlantText](https://www.planttext.com/) 


## Statistiche database 

### Crescita aspettata

| Tabella | Record iniziali | 1 Anno | 2 Anno |
|-|-|||
| Users | 100 | 1,000 | 5,000 |
| Books | 10,000 | 15,000 | 20,000 |
| Library | 500 | 5,000 | 25,000 |
| Book Reviews | 1,000 | 10,000 | 50,000 |
| Suggested Books | 5,000 | 50,000 | 250,000 |
| User Interactions | 10,000 | 100,000 | 500,000 |

### Spazio occupato

- **Users**: ~2 KB per utente → 10 MB (5,000 utenti)
- **Books**: ~1 KB per libro → 20 MB (20,000 libri)
- **Reviews**: ~0.5 KB per recensione → 25 MB (50,000 recensione)
- **Suggestions**: ~0.3 KB per suggerimento → 75 MB (250,000 suggerimento)

**Stima totale**: ~130 MB in circa 2 anni



## Troubleshooting

### Problemi comuni

#### **Violazione del vincolo di unicità**

**Problema**: Duplicato in una entry unique
**Soluzione**: Controlla se già esiste il record

#### **Errore chiave esterna**

**Problema**: Non si può aggiungere un record, la chiave riferenziata non esiste
**Soluzione**: Assicurati che il padre esista prima di creare record figli
