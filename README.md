## CachtUp
Users nowadays gets flooded with messages, news and all other kinds of information via social media applications like facebook, twitter and emails. The original purpose of connecting people gets lost in the crowd of messages. This application will address this problem by focusing on timely communication of user with people that matters.

####The backend logic for ranking contacts:
The catchup Application uses the phone call logs to decide the ranking of contacts. The history of call logs is collected in a list, allCalls. Then another list of call logs is made, which contains all calls made/received in last 3 months, we call this recentCallLogs. One more list is maintained called formerCallLogs which stores all the calls made/received in last one year. Using these list we create two features for each contact X contacted in last one year as follows:

Feature 1 :  Alpha * log((Number of  times X contacted in last one year+1) / (Number of times X is contacted in last 3 months +1))

Feature 2: Beta * log(Number of times X is contacted in all logs)
+1 factor is for smoothing

Feature 1 helps us to find those contacts which have been contacted in last one year but not so in last 3 months. Feature 2 helps us to factor in those contacts which are overall important. I collected the call logs of three different people, to learn the coefficients Alpha and Beta using Weka software. Alpha is settled for value 0.9 and Beta = 0.1

####The application tries to learn using user actions. The user has three options:
1.	Connect with the suggested contact: When a user chooses this option, the action is stored in database with the date and time of activity.  Next time, the activity is considered while generating the ranked list and the contact will have no/less priority in list.
2.	Remind me Later: When user chooses this option, it means he/she wants to contact the person, but not right now. So it is recorded in database and accordingly considered the next time user is presented with th elist.
3.	Ignore: This option simply means user does not want to contact this person at all, so the contact is deleted from the list and is not suggested again.

