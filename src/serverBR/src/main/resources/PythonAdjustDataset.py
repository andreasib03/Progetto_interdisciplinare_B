import csv

def convert_csv(input_file, output_file):
    with open(input_file, mode='r', encoding='utf-8', newline='') as infile, \
         open(output_file, mode='w', encoding='utf-8', newline='') as outfile:

        reader = csv.reader(infile, delimiter=',', quotechar='"')
        writer = csv.writer(outfile, delimiter=';', quotechar='"',
                            quoting=csv.QUOTE_MINIMAL, escapechar='\\')

        for row in reader:
            safe_row = []
            for field in row:
                # Pulizia spazi iniziali/finali
                field = field.strip()
                # Raddoppia eventuali virgolette nel contenuto
                field = field.replace('"', '""')
                safe_row.append(field)
            writer.writerow(safe_row)

    print(f"✅ File convertito da ',' a ';' con escape corretto: {output_file}")

# Esegui la conversione
convert_csv("BooksDataset.csv", "BooksDatasetClean.csv")

