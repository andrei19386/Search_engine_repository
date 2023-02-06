# The name of Project
     The project is named "SearchEngine"
 ## 1. About Project
        	The project is designed to implement a search engine for several sites.
        	The system performs search queries in Russian on several sites and displays them on the screen in descending order         	of relevance.
        	It should be noted that search queries are not automatically corrected by the system if there are typos in the query.

 ## 2. Technologies. 
		The system uses SpringBoot technology to implement responses with GET and POST requests.
        	Storing, adding, deleting data is carried out using the MySQL database management system.

 ## 3. Instruction.
		To run the project locally, you need: Edit the application.yml file located in the root of the project for a list of sites
        	to be searched.
        	To do this, you need to change the content of the sites section, respecting the indents, as it was done in the source code.
        	For example, you may write:

		
		 - url: http://lenta.ru
		   name: Lenta.ru
		
		
		Filling in all name, url entries is mandatory.
        	It is recommended to increase the size of the maximum processing data packet max_allowed_packet to 41943040 in MySQL
        	Workbench.
        	To do this, go to the Server/Status and system variables menu of the MySQL Workbench.
        	After that run the program in java for execution - run the main program: src/main/java/main/Main.java.
        	Then open the browser and type into url line: http://localhost:8080/admin.
        	On the statistics page you can see all information about sites and their status, for example:
![Statistics](https://github.com/andrei19386/Search_engine_repository/blob/master/src/main/resources/statistics.jpg)
        	If you need detailed information, you should click on the triangle on the right side of the site status: ![Detailed](https://github.com/andrei19386/Search_engine_repository/blob/master/src/main/resources/detailed.jpg)
        	Go to indexation tab. Run full site indexing. Once indexing is complete, the "Stop Indexation" button will change to "Start Indexation". Go to the search tab and type the search query into the input field, after which - type the Search button.
         	For example,
![Search](https://github.com/andrei19386/Search_engine_repository/blob/master/src/main/resources/search.jpg)

        	If you want to limit the search to one site, you can specify in the top field instead of "All Sites" a specific site
        	from the list of known sites to search.
        	If the site is not indexed, a corresponding diagnostic message will be displayed.
		There is a demonstration of the program in the file Demo.mp4.