import requests as req
from bs4 import BeautifulSoup as bs

url = 'https://www.freeclassicebooks.com/'
for letter in 'abcdefghijklmnopqrstuvwyz':
    res = req.get(url + letter + '.htm')
    soup = bs(res.text, 'html.parser')
    table = soup.find('table', {'id': 'table1'})
    rows = table.find_all('tr')
    # the third rows contains the books
    for row in rows[3:]:
        # the first column contains the book title
        title_row = str(row.find('td').text).strip().replace('\t', '').splitlines()
        title = (title_row[0] + ' ' + title_row[1]).replace('[read online]', '').\
        replace('[Read online]', '').\
        replace('[read on line]','').\
        replace('[Read on line]', '').\
        replace('  ', ' ')
        href = row.find_all('a')
        try:
            link = href[1]
        except IndexError:
            continue
        pdf_link = url +str(link.get('href')).replace('HTML', 'PDF').replace('.mht', '.pdf').replace('prcs',
                                                                                               'pdfs').replace('prc',
                                                                                                               'pdf').replace('%20', ' ').replace('../../../../Desktop/', '')
        # download the pdf
        pdf = req.get(pdf_link)

        if pdf.status_code != 200:
            continue

        try:
            text = title.split(', by ')
            author = text[1].replace('[read', '').strip()
            book_title = text[0].strip()
        except IndexError:
            text = title.split(', from ')
            try:
                author = text[1].replace('[read', '').strip()
            except IndexError:
                author = 'Unknown'
            book_title = text[0].strip()


        with open('books/' + book_title + ' by ' + author.title() + '.pdf', 'wb') as f:
            f.write(pdf.content)
