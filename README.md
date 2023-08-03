# Web-Crawler-Application
1.)Description
  This is a basic web crawler application that crawls and saves the content of crawled urls.

2.)Table of Contents
  >> The folder Crawler2 contains the main logic for web crawler.
  >> The CrawlerApp folder contains the backend and frontend of the CrawlerApplication.

3.)Usage
  The search functionality of the web crawler is implemented inside the crawler application. So the application can be used to search relevant urls saved in the      database.

4.)Technologies 
  >> Eclipse IDE is used to create the projects
  >> The database used is MySql.
  >> The frontend is made using html, css and javascript.
  >> The backend is created using SpringBoot v 3.1.2.
  
5.)Features
  >> This web crawler starts crawling from a list of starting urls.
  >> Extracts the text from the head and body section of the urls using JSoup library.
  >> To prevent duplication in the databse error handling is done in the code and the urls are saved with hash values for which guava library is used.
  >> After storing the ursl and their content the user can search for a query and the relevant urls according to the query will be returned.
  >> To provide the relevant urls calculation of relevance of a url is done by calculating the TF-IDF score of the urls.
  >> The crawling follows a breadth first search algorithm.

6.)Installation
  >> To install the project you can use Eclipse or any other IDE that supports spring boot.
  >> In the dependencies you need spring web starter dependency, mysql connector, Jsoup library, Guava library, Log4j. You can either insert the dependencies in         the pom.xml file or download the respective jar files.

7.)Usage
  For Crawler2:
  >> First load all the dependencies or jar files required.
  a.> First run the key generator class in the Crawler2 folder, it willgenerate a random key of size provided by the user.
  b.> Then run the EncryptionUtil class by providing the secret key generated. This class will encrypt the database properties file.
  c.> Finally you can run the main webcrawler class, ny providing the secret key, you can also provide urls of your choice in the Urls1.txt. You need to provide          the pages till which the crawler will go, the time till which crawler will run and the the frequency of request time.
  d.> Now you can search for a query and the craweler will return the list of relevant urls.

  For CrawlerApp: 
  a.> Run the main CrawlerApplication class and the backend server will start.
  b.> Run the frontend file index.html.
  c.> Enter a search query and the backend will provide you with a list of relevant urls.
  
8.)Contact
 This application is free to use and contribute. Anyone can contribute to this project, hence increasing its functionality and usage. 
 You can reach me at my email: 77maheshkthakur7@gmail.com
