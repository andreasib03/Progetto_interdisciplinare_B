import csv

def fix_csv_delimiter(input_file, output_file, delimiter='‰'):
    """
    Legge un file CSV con delimitatore virgola e lo riscrive con un delimitatore sicuro,
    gestendo correttamente i campi vuoti e le descrizioni che potrebbero contenere virgole o punti e virgola.
    """
    with open(input_file, 'r', encoding='utf-8') as infile, \
         open(output_file, 'w', encoding='utf-8', newline='') as outfile:

        # Usa csv.reader per gestire correttamente i campi che potrebbero contenere virgole
        reader = csv.reader(infile, delimiter=',', quotechar='"')

        # Usa csv.writer con delimitatore personalizzato
        writer = csv.writer(outfile, delimiter=delimiter, quotechar='"', quoting=csv.QUOTE_MINIMAL)

        for row in reader:
            # Gestisci i campi vuoti (stringhe vuote) e rimuovi spazi extra
            cleaned_row = [field.strip() if field.strip() else '' for field in row]
            writer.writerow(cleaned_row)

# Utilizzo
input_file = '/home/projectb/Documenti/ProjectB/serverBR/src/main/resources/modifica.csv'
output_file = '/home/projectb/Documenti/ProjectB/serverBR/src/main/resources/modifica_fixed.csv'

# Usa '‰' come delimitatore sicuro che non appare nei dati
fix_csv_delimiter(input_file, output_file, delimiter='‰')
print(f"File CSV sistemato salvato come: {output_file}")
print("NOTA: Usato '‰' come delimitatore sicuro che non appare nei dati originali")