package filmster.com.example.com.filmster;

/**
 * Created by tgreen on 8/11/15.
 */
public class Film {
    //A simple container to hold instances of different movie object data
    //in our app

    //constructor
    public Film() {
        id = null;
        title = null;
        vote_avg = null;
        synopsis = null;
        release_date = null;
        poster_path = null;
    }

    public String id;
    public String title;
    public Float vote_avg;
    public String synopsis;
    public String release_date;
    public String poster_path;
}
