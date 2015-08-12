package filmster.com.example.com.filmster;

import java.util.Date;

/**
 * Created by tgreen on 8/11/15.
 */
public class Movie {
    //A simple container to hold instances of different movie object data
    //in our app

    //constructor
    public Movie() {}

    public String id;
    public String title;
    public Float vote_avg;
    public String synopsis;
    public Date release_date;
    public String poster_path;
}
