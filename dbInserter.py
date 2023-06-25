import random
import cx_Oracle
import os


cx_Oracle.init_oracle_client(lib_dir=r"./instantclient_21_9")
connection = cx_Oracle.connect("ADMIN", "Oracleebaza1", "asedb_high")
pdfs = os.listdir('books')

for pdf in pdfs:
    title = pdf.split(' by ')[0]
    author = pdf.split(' by ')[1].replace('.pdf', '')
    with open('books/' + pdf, 'rb') as f:
        pdf_content = f.read()
    cursor = connection.cursor()
    try:
        cursor.execute("INSERT INTO books (name, author, pdf, stock) VALUES (:title, :author, :pdf, :stock)", [title,
                                                                                                          author,
                                                                                                           pdf_content, random.randint(1, 100)])
    except cx_Oracle.DatabaseError as e:
        continue
    connection.commit()
    cursor.close()