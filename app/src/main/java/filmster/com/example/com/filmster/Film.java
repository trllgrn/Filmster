package filmster.com.example.com.filmster;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by tgreen on 8/11/15.
 */
public class Film implements Parcelable {
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
        popularity = null;
        favorite = false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        else {
            Film film = (Film) o;
            return this.id.equals(film.id);
        }
    }

    //Parcelable Constructor
    private Film(Parcel in) {
        //Hey, my stuff is back. Awesome
        Object[] myStuff = in.readArray(Film.class.getClassLoader());
        //Let's open it.
        this.id = (String) myStuff[0];
        this.title = (String) myStuff[1];
        this.vote_avg = (Float) myStuff[2];
        this.synopsis = (String) myStuff[3];
        this.release_date = (String) myStuff[4];
        this.poster_path = (String) myStuff[5];
        this.popularity = (Double) myStuff[6];
        this.favorite = (Boolean) myStuff[7];
    }

    public String id;
    public String title;
    public Float vote_avg;
    public String synopsis;
    public String release_date;
    public String poster_path;
    public Double popularity;
    public Boolean favorite;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        //Dump in the data to pack up.
        //Bye, guys. Come back soon, k?
        Object[] myStuff = {id,title,vote_avg,synopsis,release_date,poster_path,popularity,favorite};
        dest.writeArray(myStuff);
    }

    public static final Parcelable.Creator<Film> CREATOR = new Parcelable.Creator<Film>() {
        @Override
        public Film createFromParcel(Parcel source) {
            return new Film(source);
        }

        @Override
        public Film[] newArray(int size) {
            return new Film[size];
        }
    };
}
