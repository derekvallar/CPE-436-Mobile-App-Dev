package edu.calpoly.android.lab4;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;


public class AdvancedJokeList extends AppCompatActivity implements JokeView.OnJokeChangeListener,
         android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor> {

    protected String TAG = "AdvJokeList";

    /**
     * Contains the name of the Author for the jokes.
     */
    protected String m_strAuthorName;

    /**
     * Adapter used to bind an AdapterView to List of Jokes.
     */
    protected JokeCursorAdapter m_jokeAdapter;

    /**
     * ViewGroup used for maintaining a list of Views that each display Jokes.
     */
    protected ListView m_vwJokeLayout;

    /**
     * EditText used for entering text for a new Joke to be added to m_arrJokeList.
     */
    protected EditText m_vwJokeEditText;

    /**
     * Button used for creating and adding a new Joke to m_arrJokeList using the
     * text entered in m_vwJokeEditText.
     */
    protected Button m_vwJokeButton;

    /**
     * Menu used for filtering Jokes.
     */
    protected Menu m_vwMenu;

    /**
     * Value used to filter which jokes get displayed to the user.
     */
    protected int m_nFilter;

    /**
     * Key used for storing and retrieving the value of m_nFilter in savedInstanceState.
     */
    protected static final String SAVED_FILTER_VALUE = "m_nFilter";

    /**
     * Key used for storing and retrieving the text in m_vwJokeEditText in savedInstanceState.
     */
    protected static final String SAVED_EDIT_TEXT = "m_vwJokeEditText";

    /**
     * Menu/Submenu MenuItem IDs.
     */
    protected static final int FILTER = Menu.FIRST;
    protected static final int FILTER_LIKE = SubMenu.FIRST;
    protected static final int FILTER_DISLIKE = SubMenu.FIRST + 1;
    protected static final int FILTER_UNRATED = SubMenu.FIRST + 2;
    protected static final int FILTER_SHOW_ALL = SubMenu.FIRST + 3;

    /**
     * Used to handle Contextual Action Mode when long-clicking on a single Joke.
     */
    private ActionMode.Callback mActionModeCallback;
    private ActionMode mActionMode;

    /**
     * The Joke that is currently focused after long-clicking.
     */
    private int selected_position;

    /**
     * The ID of the CursorLoader to be initialized in the LoaderManager and used to load a Cursor.
     */
    private static final int LOADER_ID = 1;

    /**
     * The String representation of the Show All filter. The Show All case
     * needs a String representation of a value that is different from
     * Joke.LIKE, Joke.DISLIKE and Joke.UNRATED. The actual value doesn't
     * matter as long as it's different, since the WHERE clause is set to
     * null when making database operations under this setting.
     */
    public static final String SHOW_ALL_FILTER_STRING = "" + FILTER_SHOW_ALL;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String temp = "";

        super.onCreate(savedInstanceState);

        this.m_jokeAdapter = new JokeCursorAdapter(this, null, 0);
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

        this.m_strAuthorName = this.getResources().getString(R.string.author_name);
        this.m_nFilter = FILTER_SHOW_ALL;

        initLayout();
        initAddJokeListeners();

//        for (String s : this.getResources().getStringArray(R.array.jokeList)) {
//            addJoke(new Joke(s, this.m_strAuthorName));
//        }

        getPreferences(MODE_PRIVATE).getString(SAVED_EDIT_TEXT, temp);
        Log.d(TAG, "onCreate: saved text = " + temp + ".");
        m_vwJokeEditText.setText(temp);

        this.m_jokeAdapter.setOnJokeChangeListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_FILTER_VALUE, m_nFilter);

        super.onSaveInstanceState(outState);
    }

    /**
     * This method is called after {@link #onStart} when the activity is
     * being re-initialized from a previously saved state, given here in
     * <var>savedInstanceState</var>.  Most implementations will simply use {@link #onCreate}
     * to restore their state, but it is sometimes convenient to do it here
     * after all of the initialization has been done or to allow subclasses to
     * decide whether to use your default implementation.  The default
     * implementation of this method performs a restore of any view state that
     * had previously been frozen by {@link #onSaveInstanceState}.
     * <p>
     * <p>This method is called between {@link #onStart} and
     * {@link #onPostCreate}.
     *
     * @param savedInstanceState the data most recently supplied in {@link #onSaveInstanceState}.
     * @see #onCreate
     * @see #onPostCreate
     * @see #onResume
     * @see #onSaveInstanceState
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVED_FILTER_VALUE)) {
                m_nFilter = savedInstanceState.getInt(SAVED_FILTER_VALUE);
                filterJokeList(m_nFilter);
            }
        }

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        this.m_vwMenu = menu;
        return true;
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {

        Log.d(TAG, "onPause: saving = " + m_vwJokeEditText.getText().toString() + ".");
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(SAVED_EDIT_TEXT, m_vwJokeEditText.getText().toString());
        editor.apply();
        super.onPause();
    }

    /**
     * Prepare the Screen's standard options menu to be displayed.  This is
     * called right before the menu is shown, every time it is shown.  You can
     * use this method to efficiently enable/disable items or otherwise
     * dynamically modify the contents.
     * <p>
     * <p>The default implementation updates the system menu items based on the
     * activity's state.  Deriving classes should always call through to the
     * base class implementation.
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_filter).setTitle(getMenuTitleChange());
        m_vwMenu = menu;
        return super.onPrepareOptionsMenu(menu);
    }

    public String getMenuTitleChange() {
        Log.d(TAG, "getMenuTitleChange: m_nFilter = " + m_nFilter);
        switch (m_nFilter) {
            case FILTER_LIKE:
                return this.getResources().getString(R.string.like_menuitem);

            case FILTER_DISLIKE:
                return this.getResources().getString(R.string.dislike_menuitem);

            case FILTER_SHOW_ALL:
                return this.getResources().getString(R.string.show_all_menuitem);

            case FILTER_UNRATED:
                return this.getResources().getString(R.string.unrated_menuitem);

            default:
                return "";
        }
    }

    /**
     * Method is used to encapsulate the code that initializes and sets the
     * Layout for this Activity.
     */
    protected void initLayout() {
        this.setContentView(R.layout.advanced);
        this.m_vwJokeLayout = (ListView) this.findViewById(R.id.jokeListViewGroup);
        if (this.m_vwJokeLayout != null) {
            this.m_vwJokeLayout.setAdapter(m_jokeAdapter);
        }

        this.m_vwJokeLayout.setOnItemLongClickListener(new OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) {
                    return false;
                }
                mActionMode = startSupportActionMode(mActionModeCallback);
                selected_position = position;
                return true;
            }
        });

        this.m_vwJokeEditText = (EditText) this.findViewById(R.id.newJokeEditText);
        this.m_vwJokeButton = (Button) this.findViewById(R.id.addJokeButton);

        mActionModeCallback = new ActionMode.Callback() {
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.actionmenu, menu);
                return true;
            }

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_remove:
                        removeJoke(((JokeView) m_vwJokeLayout.getChildAt(selected_position)).getJoke());
                        mode.finish();
                        return true;

                    default:
                        return false;
                }
            }

            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
            }
        };
    }

    /**
     * Method is used to encapsulate the code that initializes and sets the
     * Event Listeners which will respond to requests to "Add" a new Joke to the
     * list.
     */
    protected void initAddJokeListeners() {
        this.m_vwJokeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                String jokeText = m_vwJokeEditText.getText().toString();
                if (!jokeText.equals("")) {
                    addJoke(new Joke(jokeText, m_strAuthorName));
                    m_vwJokeEditText.setText("");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(m_vwJokeEditText.getWindowToken(), 0);
                }
            }
        });

        this.m_vwJokeEditText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String jokeText = m_vwJokeEditText.getText().toString();
                    if (jokeText != null && !jokeText.equals("")) {
                        addJoke(new Joke(jokeText, m_strAuthorName));
                        m_vwJokeEditText.setText("");
                        return true;
                    }
                }
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(m_vwJokeEditText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Method used for encapsulating the logic necessary to properly add a new
     * Joke to m_arrJokeList, and display it on screen.
     *
     * @param joke The Joke to add to list of Jokes.
     */
    protected void addJoke(Joke joke) {
        Uri uri = Uri.parse("content://edu.calpoly.android.lab4.contentprovider/joke_table/jokes/" + joke.getID());
        ContentValues cv = new ContentValues();
        cv.put(JokeTable.JOKE_KEY_AUTHOR, joke.getAuthor());
        cv.put(JokeTable.JOKE_KEY_RATING, joke.getRating());
        cv.put(JokeTable.JOKE_KEY_TEXT, joke.getJoke());

        Uri id = getContentResolver().insert(uri, cv);
        joke.setID(ContentUris.parseId(id));
        fillData();
        Log.d(TAG, "addJoke: " + ContentUris.parseId(id) + ", actual: " + joke.getID());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.submenu_like:
                Log.d(TAG, "onOptionsItemSelected: like");
                filterJokeList(FILTER_LIKE);
                m_nFilter = FILTER_LIKE;
                break;

            case R.id.submenu_dislike:
                Log.d(TAG, "onOptionsItemSelected: dislike");
                filterJokeList(FILTER_DISLIKE);
                m_nFilter = FILTER_DISLIKE;
                break;

            case R.id.submenu_unrated:
                Log.d(TAG, "onOptionsItemSelected: unrated");
                filterJokeList(FILTER_UNRATED);
                m_nFilter = FILTER_UNRATED;
                break;

            case R.id.submenu_show_all:
                Log.d(TAG, "onOptionsItemSelected: showall");
                filterJokeList(FILTER_SHOW_ALL);
                m_nFilter = FILTER_SHOW_ALL;
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        m_vwMenu.findItem(R.id.menu_filter).setTitle(getMenuTitleChange());
        return true;
    }

    private void filterJokeList(int filter) {
        Log.d(TAG, "filterJokeList: " + filter);
        m_nFilter = filter;
        fillData();
    }

//    private void syncFilterChanges() {
//        for (Joke j : this.m_arrFilteredJokeList) {
//            //Has the joke in it already, need rating change
//            if (this.m_arrJokeList.contains(j)) {
//                this.m_arrJokeList.get(this.m_arrJokeList.indexOf(j)).setRating(j.getRating());
//            }
//        }
//    }

    protected void removeJoke(Joke jv) {
        Log.d(TAG, "removeJoke: " + jv.getID());
        Uri uri = Uri.parse("content://edu.calpoly.android.lab4.contentprovider/joke_table/jokes/" + jv.getID());
        getContentResolver().delete(uri, null, null);
        fillData();

    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader: ");
        String[] projection = {JokeTable.JOKE_KEY_ID, JokeTable.JOKE_KEY_TEXT, JokeTable.JOKE_KEY_RATING, JokeTable.JOKE_KEY_AUTHOR};
        String filterNum;


        switch (m_nFilter) {
            case Joke.UNRATED:
                filterNum = Joke.UNRATED + "";
                break;

            case Joke.LIKE:
                filterNum = Joke.LIKE + "";
                break;

            case Joke.DISLIKE:
                filterNum = Joke.DISLIKE + "";
                break;

            default:
                filterNum = SHOW_ALL_FILTER_STRING;
        }
        Uri uri = Uri.parse("content://edu.calpoly.android.lab4.contentprovider/joke_table/filters/" + filterNum);
        return new CursorLoader(this, uri, projection, null, null, null);
    }

    /**
     * Called when a previously created loader has finished its load.  Note
     * that normally an application is <em>not</em> allowed to commit fragment
     * transactions while in this call, since it can happen after an
     * activity's state is saved.  See {@link FragmentManager#beginTransaction()
     * FragmentManager.openTransaction()} for further discussion on this.
     * <p/>
     * <p>This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.  In particular:
     * <p/>
     * <ul>
     * <li> <p>The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.  For example, if the data is a {@link Cursor}
     * and you place it in a {@link CursorAdapter}, use
     * the {@link CursorAdapter#CursorAdapter(Context,
     * Cursor, int)} constructor <em>without</em> passing
     * in either {@link CursorAdapter#FLAG_AUTO_REQUERY}
     * or {@link CursorAdapter#FLAG_REGISTER_CONTENT_OBSERVER}
     * (that is, use 0 for the flags argument).  This prevents the CursorAdapter
     * from doing its own observing of the Cursor, which is not needed since
     * when a change happens you will get a new Cursor throw another call
     * here.
     * <li> The Loader will release the data once it knows the application
     * is no longer using it.  For example, if the data is
     * a {@link Cursor} from a {@link CursorLoader},
     * you should not call close() on it yourself.  If the Cursor is being placed in a
     * {@link CursorAdapter}, you should use the
     * {@link CursorAdapter#swapCursor(Cursor)}
     * method so that the old Cursor is not closed.
     * </ul>
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        m_jokeAdapter.swapCursor(data);
        m_jokeAdapter.setOnJokeChangeListener(this);
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        m_jokeAdapter.swapCursor(null);
    }

    public void fillData() {
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        this.m_vwJokeLayout.setAdapter(m_jokeAdapter);

    }

    /**
     * Called when the underlying Joke in a JokeView object changes state.
     *
     * @param view The JokeView in which the Joke was changed.
     * @param joke
     */
    @Override
    public void onJokeChanged(JokeView view, Joke joke) {
        Log.d(TAG, "onJokeChanged: " + joke.toString() + ", rating: " + joke.getRating());
        Uri uri = Uri.parse("content://edu.calpoly.android.lab4.contentprovider/joke_table/jokes/" + joke.getID());
        ContentValues cv = new ContentValues();
        cv.put(JokeTable.JOKE_KEY_AUTHOR, joke.getAuthor());
        cv.put(JokeTable.JOKE_KEY_RATING, joke.getRating());
        cv.put(JokeTable.JOKE_KEY_TEXT, joke.getJoke());

        getContentResolver().update(uri, cv, null, null);
        m_jokeAdapter.setOnJokeChangeListener(null);

        fillData();
    }
}