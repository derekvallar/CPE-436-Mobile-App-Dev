package edu.calpoly.android.lab4;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

import edu.calpoly.android.lab4.JokeView.OnJokeChangeListener;

/**
 * Class that functions similarly to JokeListAdapter, but instead uses a Cursor.
 * A Cursor is a list of rows from a database that acts as a medium between the
 * database and a ViewGroup (in this case, a SQLite database table containing rows
 * of jokes and a ListView containing JokeViews).
 */
public class JokeCursorAdapter extends android.support.v4.widget.CursorAdapter {

	/** The OnJokeChangeListener that should be connected to each of the
	 * JokeViews created/managed by this Adapter. */
	private OnJokeChangeListener m_listener;

	/**
	 * Parameterized constructor that takes in the application Context in which
	 * it is being used and the Collection of Joke objects to which it is bound.
	 * 
	 * @param context
	 *            The application Context in which this JokeListAdapter is being
	 *            used.
	 * 
	 * @param jokeCursor
	 *            A Database Cursor containing a result set of Jokes which
	 *            should be bound to JokeViews.
	 *            
	 * @param flags
	 * 			  A list of flags that decide this adapter's behavior.
	 */
	public JokeCursorAdapter(Context context, Cursor jokeCursor, int flags) {
		super(context, jokeCursor, flags);

	}

	/**
	 * Mutator method for changing the OnJokeChangeListener.
	 * 
	 * @param listener
	 *            The OnJokeChangeListener that will be notified when the
	 *            internal state of any Joke contained in one of this Adapters
	 *            JokeViews is changed.
	 */
	public void setOnJokeChangeListener(OnJokeChangeListener mListener) {
		this.m_listener = mListener;
	}

    /**
     * Bind an existing view to the data pointed to by cursor
     *
     * @param view    Existing view, returned earlier by newView
     * @param context Interface to application's global information
     * @param cursor  The cursor from which to get the data. The cursor is already
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        JokeView jokeview = (JokeView) view;
        jokeview.setOnJokeChangeListener(null);

        Joke temp = new Joke();
		temp.setID(cursor.getInt(JokeTable.JOKE_COL_ID));
        temp.setJoke(cursor.getString(JokeTable.JOKE_COL_TEXT));
        temp.setRating(cursor.getInt(JokeTable.JOKE_COL_RATING));
        temp.setAuthor(cursor.getString(JokeTable.JOKE_COL_AUTHOR));

        jokeview.setJoke(temp);
        jokeview.setOnJokeChangeListener(m_listener);
    }

    /**
     * Makes a new view to hold the data pointed to by cursor.
     *
     * @param context Interface to application's global information
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        Joke temp = new Joke();
		temp.setID(cursor.getInt(JokeTable.JOKE_COL_ID));
		temp.setJoke(cursor.getString(JokeTable.JOKE_COL_TEXT));
        temp.setRating(cursor.getInt(JokeTable.JOKE_COL_RATING));
        temp.setAuthor(cursor.getString(JokeTable.JOKE_COL_AUTHOR));

		JokeView jokeview = new JokeView(context, temp);
        jokeview.setOnJokeChangeListener(m_listener);

        return jokeview;
    }
}