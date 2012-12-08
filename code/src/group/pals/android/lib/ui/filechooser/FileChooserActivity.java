/*
 *    Copyright (c) 2012 Hai Bison
 *
 *    See the file LICENSE at the root directory of this project for copying
 *    permission.
 */

package group.pals.android.lib.ui.filechooser;

import group.pals.android.lib.ui.filechooser.io.IFile;
import group.pals.android.lib.ui.filechooser.io.IFileFilter;
import group.pals.android.lib.ui.filechooser.prefs.DisplayPrefs;
import group.pals.android.lib.ui.filechooser.providers.BaseFileProviderUtils;
import group.pals.android.lib.ui.filechooser.providers.ProviderUtils;
import group.pals.android.lib.ui.filechooser.providers.basefile.BaseFileContract.BaseFile;
import group.pals.android.lib.ui.filechooser.providers.localfile.LocalFileContract;
import group.pals.android.lib.ui.filechooser.services.LocalFileProvider;
import group.pals.android.lib.ui.filechooser.utils.ActivityCompat;
import group.pals.android.lib.ui.filechooser.utils.E;
import group.pals.android.lib.ui.filechooser.utils.FileUtils;
import group.pals.android.lib.ui.filechooser.utils.Ui;
import group.pals.android.lib.ui.filechooser.utils.Utils;
import group.pals.android.lib.ui.filechooser.utils.history.History;
import group.pals.android.lib.ui.filechooser.utils.history.HistoryFilter;
import group.pals.android.lib.ui.filechooser.utils.history.HistoryListener;
import group.pals.android.lib.ui.filechooser.utils.history.HistoryStore;
import group.pals.android.lib.ui.filechooser.utils.ui.Dlg;
import group.pals.android.lib.ui.filechooser.utils.ui.LoadingDialog;
import group.pals.android.lib.ui.filechooser.utils.ui.TaskListener;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Main activity for this library.<br>
 * <br>
 * <b>Notes:</b><br>
 * <br>
 * <b>I.</b> About keys {@link FileChooserActivity#_Rootpath},
 * {@link FileChooserActivity#_SelectFile} and preference
 * {@link DisplayPrefs#isRememberLastLocation(Context)}, the priorities of them
 * are:<br>
 * <li>1. {@link FileChooserActivity#_SelectFile}</li>
 * 
 * <li>2. {@link DisplayPrefs#isRememberLastLocation(Context)}</li>
 * 
 * <li>3. {@link FileChooserActivity#_Rootpath}</li>
 * 
 * @author Hai Bison
 * 
 */
public class FileChooserActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The full name of this class. Generally used for debugging.
     */
    private static final String _ClassName = FileChooserActivity.class.getName();

    /**
     * Types of view.
     * 
     * @author Hai Bison
     * @since v4.0 beta
     */
    public static enum ViewType {
        /**
         * Use {@link ListView} to display file list.
         */
        List,
        /**
         * Use {@link GridView} to display file list.
         */
        Grid
    }

    /*---------------------------------------------
     * KEYS
     */

    /**
     * Sets value of this key to a theme in {@code android.R.style.Theme_*}.<br>
     * Default is:<br>
     * 
     * <li>{@link android.R.style#Theme_DeviceDefault} for {@code SDK >= }
     * {@link Build.VERSION_CODES#ICE_CREAM_SANDWICH}</li>
     * 
     * <li>{@link android.R.style#Theme_Holo} for {@code SDK >= }
     * {@link Build.VERSION_CODES#HONEYCOMB}</li>
     * 
     * <li>{@link android.R.style#Theme} for older systems</li>
     * 
     * @since v4.3 beta
     */
    public static final String _Theme = _ClassName + ".theme";

    /**
     * Key to hold the root path.<br>
     * <br>
     * If {@link LocalFileProvider} is used, then default is sdcard, if sdcard
     * is not available, "/" will be used.<br>
     * <br>
     * <b>Note</b>: The value of this key is a {@link Uri}
     */
    public static final String _Rootpath = _ClassName + ".rootpath";

    /**
     * Key to hold the authority of file provider.<br>
     * Default is {@link LocalFileContract#_Authority}.
     */
    public static final String _FileProviderAuthority = _ClassName + ".file_provider_authority";

    // ---------------------------------------------------------

    /**
     * Key to hold filter mode, can be one of
     * {@link BaseFile#_FilterDirectoriesOnly},
     * {@link BaseFile#_FilterFilesAndDirectories},
     * {@link BaseFile#_FilterFilesOnly}. Default is
     * {@link BaseFile#_FilterFilesOnly}.
     */
    public static final String _FilterMode = _ClassName + ".filter_mode";

    // flags

    // ---------------------------------------------------------

    /**
     * Key to hold max file count that's allowed to be listed, default =
     * {@code 1000}.
     */
    public static final String _MaxFileCount = _ClassName + ".max_file_count";
    /**
     * Key to hold multi-selection mode, default = {@code false}
     */
    public static final String _MultiSelection = _ClassName + ".multi_selection";
    /**
     * Key to hold the component class implementing {@link IFileFilter}, default
     * is {@code null}.
     * 
     * @since v5.1 beta
     */
    @Deprecated
    public static final String _IFileFilterClass = _ClassName + ".ifile_filter_class";
    /**
     * Key to hold display-hidden-files, default = {@code false}
     */
    public static final String _DisplayHiddenFiles = _ClassName + ".display_hidden_files";
    /**
     * Sets this to {@code true} to enable double tapping to choose files/
     * directories. In older versions, double tapping is default. However, since
     * v4.7 beta, single tapping is default. So if you want to keep the old way,
     * please set this key to {@code true}.
     * 
     * @since v4.7 beta
     */
    public static final String _DoubleTapToChooseFiles = _ClassName + ".double_tap_to_choose_files";
    /**
     * Sets the file you want to select when starting this activity. This is a
     * {@link Uri}.<br>
     * <b>Notes:</b><br>
     * <li>Currently this key is only used for single selection mode.</li>
     * 
     * <li>If you use save dialog mode, this key will override key
     * {@link #_DefaultFilename}.</li>
     * 
     * @since v4.7 beta
     */
    public static final String _SelectFile = _ClassName + ".select_file";

    // ---------------------------------------------------------

    /**
     * Key to hold property save-dialog, default = {@code false}
     */
    public static final String _SaveDialog = _ClassName + ".save_dialog";
    /**
     * Key to hold default filename, default = {@code null}
     */
    public static final String _DefaultFilename = _ClassName + ".default_filename";
    /**
     * Key to hold results (can be one or multiple files)
     */
    public static final String _Results = _ClassName + ".results";

    /**
     * This key holds current location (a {@link Uri}), to restore it after
     * screen orientation changed.
     */
    static final String _CurrentLocation = _ClassName + ".current_location";
    /**
     * This key holds current history (a {@link History}&lt;{@link IFile}&gt;),
     * to restore it after screen orientation changed
     */
    static final String _History = _ClassName + ".history";

    static final String _Path = _ClassName + ".path";

    // ====================
    // "CONSTANT" VARIABLES

    private String mFileProviderAuthority;
    private Uri mRoot;
    private int mFilterMode;
    private int mMaxFileCount;
    private int mSortBy;
    private boolean mSortAscending;
    private boolean mIsMultiSelection;
    private boolean mIsSaveDialog;
    private boolean mDoubleTapToChooseFiles;

    /**
     * The history.
     */
    private History<Uri> mHistory;

    /**
     * The adapter of list view.
     */
    private BaseFileAdapter mFileAdapter;

    /*
     * controls
     */
    private HorizontalScrollView mViewLocationsContainer;
    private ViewGroup mViewLocations;
    private ViewGroup mViewFilesContainer;
    private TextView mTxtFullDirName;
    private AbsListView mViewFiles;
    private TextView mFooterView;
    private View mViewLoading;
    private Button mBtnOk;
    private EditText mTxtSaveas;
    private ImageView mViewGoBack;
    private ImageView mViewGoForward;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*
         * THEME
         */

        if (getIntent().hasExtra(_Theme)) {
            int theme;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                theme = getIntent().getIntExtra(_Theme, android.R.style.Theme_DeviceDefault);
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                theme = getIntent().getIntExtra(_Theme, android.R.style.Theme_Holo);
            else
                theme = getIntent().getIntExtra(_Theme, android.R.style.Theme);
            setTheme(theme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.afc_file_chooser);

        initGestureDetector();

        // load configurations

        mFileProviderAuthority = getIntent().getStringExtra(_FileProviderAuthority);
        if (mFileProviderAuthority == null)
            mFileProviderAuthority = LocalFileContract._Authority;

        mIsMultiSelection = getIntent().getBooleanExtra(_MultiSelection, false);

        mIsSaveDialog = getIntent().getBooleanExtra(_SaveDialog, false);
        if (mIsSaveDialog)
            mIsMultiSelection = false;

        mDoubleTapToChooseFiles = getIntent().getBooleanExtra(_DoubleTapToChooseFiles, false);

        mRoot = (Uri) getIntent().getSerializableExtra(_Rootpath);
        mFilterMode = getIntent().getIntExtra(_FilterMode, BaseFile._FilterFilesOnly);
        mMaxFileCount = getIntent().getIntExtra(_MaxFileCount, 1000);
        mFileAdapter = new BaseFileAdapter(this, mFilterMode, mIsMultiSelection);

        // load controls

        mViewGoBack = (ImageView) findViewById(R.id.afc_filechooser_activity_button_go_back);
        mViewGoForward = (ImageView) findViewById(R.id.afc_filechooser_activity_button_go_forward);
        mViewLocations = (ViewGroup) findViewById(R.id.afc_filechooser_activity_view_locations);
        mViewLocationsContainer = (HorizontalScrollView) findViewById(R.id.afc_filechooser_activity_view_locations_container);
        mTxtFullDirName = (TextView) findViewById(R.id.afc_filechooser_activity_textview_full_dir_name);
        mViewFilesContainer = (ViewGroup) findViewById(R.id.afc_filechooser_activity_view_files_container);
        mFooterView = (TextView) findViewById(R.id.afc_filechooser_activity_view_files_footer_view);
        mViewLoading = findViewById(R.id.afc_filechooser_activity_view_loading);
        mTxtSaveas = (EditText) findViewById(R.id.afc_filechooser_activity_textview_saveas_filename);
        mBtnOk = (Button) findViewById(R.id.afc_filechooser_activity_button_ok);

        // history
        if (savedInstanceState != null && savedInstanceState.get(_History) instanceof HistoryStore<?>)
            mHistory = savedInstanceState.getParcelable(_History);
        else
            mHistory = new HistoryStore<Uri>();
        mHistory.addListener(new HistoryListener<Uri>() {

            @Override
            public void onChanged(History<Uri> history) {
                int idx = history.indexOf(mFileAdapter.getPath());
                mViewGoBack.setEnabled(idx > 0);
                mViewGoForward.setEnabled(idx >= 0 && idx < history.size() - 1);
            }
        });

        // make sure RESULT_CANCELED is default
        setResult(RESULT_CANCELED);

        setupHeader();
        setupViewFiles();
        setupFooter();

        loadInitialPath(savedInstanceState);
    }// onCreate()

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.afc_file_chooser_activity, menu);
        return true;
    }// onCreateOptionsMenu()

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /*
         * sorting
         */

        final boolean _sortAscending = DisplayPrefs.isSortAscending(this);
        MenuItem miSort = menu.findItem(R.id.afc_filechooser_activity_menuitem_sort);

        switch (DisplayPrefs.getSortType(this)) {
        case BaseFile._SortByName:
            miSort.setIcon(_sortAscending ? R.drawable.afc_ic_menu_sort_by_name_asc
                    : R.drawable.afc_ic_menu_sort_by_name_desc);
            break;
        case BaseFile._SortBySize:
            miSort.setIcon(_sortAscending ? R.drawable.afc_ic_menu_sort_by_size_asc
                    : R.drawable.afc_ic_menu_sort_by_size_desc);
            break;
        case BaseFile._SortByModificationTime:
            miSort.setIcon(_sortAscending ? R.drawable.afc_ic_menu_sort_by_date_asc
                    : R.drawable.afc_ic_menu_sort_by_date_desc);
            break;
        }

        /*
         * view type
         */

        MenuItem menuItem = menu.findItem(R.id.afc_filechooser_activity_menuitem_switch_viewmode);
        switch (DisplayPrefs.getViewType(this)) {
        case Grid:
            menuItem.setIcon(R.drawable.afc_ic_menu_listview);
            menuItem.setTitle(R.string.afc_cmd_list_view);
            break;
        case List:
            menuItem.setIcon(R.drawable.afc_ic_menu_gridview);
            menuItem.setTitle(R.string.afc_cmd_grid_view);
            break;
        }

        return true;
    }// onPrepareOptionsMenu()

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getGroupId() == R.id.afc_filechooser_activity_menugroup_sorter) {
            doResortViewFiles();
        }// group_sorter
        else if (item.getItemId() == R.id.afc_filechooser_activity_menuitem_new_folder) {
            doCreateNewDir();
        } else if (item.getItemId() == R.id.afc_filechooser_activity_menuitem_switch_viewmode) {
            doSwitchViewType();
        } else if (item.getItemId() == R.id.afc_filechooser_activity_menuitem_home) {
            doGoHome();
        } else if (item.getItemId() == R.id.afc_filechooser_activity_menuitem_reload) {
            doReloadCurrentLocation();
        }

        return true;
    }// onOptionsItemSelected()

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }// onConfigurationChanged()

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(_CurrentLocation, mFileAdapter.getPath());
        outState.putParcelable(_History, mHistory);
    }// onSaveInstanceState()

    @Override
    protected void onStart() {
        super.onStart();
        if (!mIsMultiSelection && !mIsSaveDialog && mDoubleTapToChooseFiles)
            Dlg.toast(this, R.string.afc_hint_double_tap_to_select_file, Dlg._LengthShort);
    }// onStart()

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }// onDestroy()

    /*
     * LOADERMANAGER.LOADERCALLBACKS
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        mViewFilesContainer.setVisibility(View.GONE);
        mFooterView.setVisibility(View.GONE);
        mViewLoading.setVisibility(View.VISIBLE);

        return new CursorLoader(this, BaseFile.genContentUriBase(mFileProviderAuthority).buildUpon()
                .appendPath(args.getString(_Path)).build(), null, null, null, null);
    }// onCreateLoader()

    @Override
    public void onLoadFinished(Loader<Cursor> loader, final Cursor data) {
        // TODO
        if (data == null)
            return;

        final Uri _lastPath = mFileAdapter.getPath();

        // update list view
        mFileAdapter.changeCursor(data);

        // update footers

        data.moveToLast();
        final Uri _uriInfo = Uri.parse(data.getString(data.getColumnIndex(BaseFile._ColumnUri)));

        mFooterView.setVisibility(ProviderUtils.getBooleanQueryParam(_uriInfo, BaseFile._ParamHasMoreFiles)
                || mFileAdapter.isEmpty() ? View.VISIBLE : View.GONE);
        if (ProviderUtils.getBooleanQueryParam(_uriInfo, BaseFile._ParamHasMoreFiles))
            mFooterView.setText(getString(R.string.afc_pmsg_max_file_count_allowed, mMaxFileCount));
        else if (mFileAdapter.isEmpty())
            mFooterView.setText(R.string.afc_msg_empty);

        final Uri _selectedFile = (Uri) getIntent().getParcelableExtra(_SelectFile);
        if (_selectedFile != null)
            getIntent().removeExtra(_SelectFile);

        /*
         * We use a Runnable to make sure this work. Because if the list view is
         * handling data, this might not work.
         */
        mViewFiles.post(new Runnable() {

            @Override
            public void run() {
                final int _colUri = data.getColumnIndex(BaseFile._ColumnUri);

                int shouldBeSelectedIdx = -1;
                Uri uri = _selectedFile != null ? _selectedFile : _lastPath;

                if (!BaseFileProviderUtils.fileExists(FileChooserActivity.this, mFileProviderAuthority, uri))
                    return;
                Uri parentUri = BaseFileProviderUtils.getParentFile(FileChooserActivity.this, mFileProviderAuthority,
                        uri);
                if (parentUri == null
                        || !parentUri.getLastPathSegment().equals(mFileAdapter.getPath().getLastPathSegment()))
                    return;

                if (uri != null) {
                    if (data.moveToFirst()) {
                        while (!data.isLast()) {
                            Uri u = Uri.parse(data.getString(_colUri));
                            if (uri.getLastPathSegment().equals(u.getLastPathSegment())) {
                                if (uri != _lastPath
                                        || BaseFileProviderUtils.isDirectory(FileChooserActivity.this,
                                                mFileProviderAuthority, u))
                                    shouldBeSelectedIdx = data.getPosition();
                                break;
                            }
                            data.moveToNext();
                        }
                    }
                }

                if (shouldBeSelectedIdx >= 0 && shouldBeSelectedIdx < mFileAdapter.getCount())
                    mViewFiles.setSelection(shouldBeSelectedIdx);
                else if (!mFileAdapter.isEmpty())
                    mViewFiles.setSelection(0);
            }// run()
        });

        /*
         * navigation buttons
         */
        createLocationButtons(mFileAdapter.getPath());

        if (_selectedFile != null && mIsSaveDialog
                && BaseFileProviderUtils.isFile(this, mFileProviderAuthority, _selectedFile))
            mTxtSaveas.setText(_selectedFile.getLastPathSegment());

        /*
         * Don't push current location into history.
         */
        if (mFileAdapter.getPath().equals(getIntent().getParcelableExtra(_CurrentLocation))) {
            getIntent().removeExtra(_CurrentLocation);
            mHistory.notifyHistoryChanged();
        } else {
            mHistory.push(mFileAdapter.getPath());
        }

        boolean hasMoreFiles = ProviderUtils.getBooleanQueryParam(_uriInfo, BaseFile._ParamHasMoreFiles);
        showFooterView(hasMoreFiles || mFileAdapter.isEmpty(),
                hasMoreFiles ? getString(R.string.afc_pmsg_max_file_count_allowed, mMaxFileCount)
                        : getString(R.string.afc_msg_empty), mFileAdapter.isEmpty());

        mViewFilesContainer.setVisibility(View.VISIBLE);
        mViewLoading.setVisibility(View.GONE);
    }// onLoadFinished()

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFileAdapter.changeCursor(null);
        mViewFilesContainer.setVisibility(View.GONE);
        mFooterView.setVisibility(View.GONE);
        mViewLoading.setVisibility(View.VISIBLE);
    }// onLoaderReset()

    /**
     * Connects to file provider service, then loads root directory. If can not,
     * then finishes this activity with result code =
     * {@link Activity#RESULT_CANCELED}
     * 
     * @param savedInstanceState
     */
    private void loadInitialPath(final Bundle savedInstanceState) {
        /*
         * Priorities for starting path:
         * 
         * 1. Current location (in case the activity has been killed after
         * configurations changed).
         * 
         * 2. Selected file from key _SelectFile.
         * 
         * 3. Last location.
         * 
         * 4. Root path from key _Rootpath.
         */

        // current location
        Uri path = (Uri) (savedInstanceState != null ? savedInstanceState.getParcelable(_CurrentLocation) : null);

        // selected file
        if (path == null) {
            path = (Uri) getIntent().getParcelableExtra(_SelectFile);
            if (path != null && BaseFileProviderUtils.fileExists(this, mFileProviderAuthority, path))
                path = BaseFileProviderUtils.getParentFile(this, mFileProviderAuthority, path);
        }

        // last location
        if (path == null && DisplayPrefs.isRememberLastLocation(this)) {
            String lastLocation = DisplayPrefs.getLastLocation(this);
            if (lastLocation != null)
                path = Uri.parse(lastLocation);
        }

        if (path == null || !BaseFileProviderUtils.isDirectory(this, mFileProviderAuthority, path)) {
            path = mRoot;
            if (path == null || !BaseFileProviderUtils.isDirectory(this, mFileProviderAuthority, path))
                path = BaseFileProviderUtils.getDefaultPath(this, mFileProviderAuthority);
            if (path == null) {
                // TODO: show message/ toast
                finish();
            }
        }

        if (!BaseFileProviderUtils.fileCanRead(this, mFileProviderAuthority, path)) {
            Dlg.toast(FileChooserActivity.this,
                    getString(R.string.afc_pmsg_cannot_access_dir, path.getLastPathSegment()), Dlg._LengthShort);
            finish();
        }

        /*
         * Prepare the loader. Either re-connect with an existing one, or start
         * a new one.
         */
        Bundle b = new Bundle();
        b.putString(_Path, path.toString());
        getSupportLoaderManager().initLoader(0, b, this);
    }// loadInitialPath()

    /**
     * Setup:<br>
     * - title of activity;<br>
     * - button go back;<br>
     * - button location;<br>
     * - button go forward;
     */
    private void setupHeader() {
        if (mIsSaveDialog) {
            setTitle(R.string.afc_title_save_as);
        } else {
            switch (mFilterMode) {
            case BaseFile._FilterFilesOnly:
                setTitle(R.string.afc_title_choose_files);
                break;
            case BaseFile._FilterFilesAndDirectories:
                setTitle(R.string.afc_title_choose_files_and_directories);
                break;
            case BaseFile._FilterDirectoriesOnly:
                setTitle(R.string.afc_title_choose_directories);
                break;
            }
        }// title of activity

        mViewGoBack.setEnabled(false);
        mViewGoBack.setOnClickListener(mBtnGoBackOnClickListener);

        mViewGoForward.setEnabled(false);
        mViewGoForward.setOnClickListener(mBtnGoForwardOnClickListener);

        for (ImageView v : new ImageView[] { mViewGoBack, mViewGoForward })
            v.setOnLongClickListener(mBtnGoBackForwardOnLongClickListener);
    }// setupHeader()

    /**
     * Setup:<br>
     * - {@link #mViewFiles}<br>
     * - {@link #mViewFilesContainer}<br>
     * - {@link #mFileAdapter}
     */
    private void setupViewFiles() {
        switch (DisplayPrefs.getViewType(this)) {
        case Grid:
            mViewFiles = (AbsListView) getLayoutInflater().inflate(R.layout.afc_gridview_files, null);
            break;
        case List:
            mViewFiles = (AbsListView) getLayoutInflater().inflate(R.layout.afc_listview_files, null);
            break;
        }

        mViewFilesContainer.removeAllViews();
        mViewFilesContainer.addView(mViewFiles, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 1));

        mViewFiles.setOnItemClickListener(mViewFilesOnItemClickListener);
        mViewFiles.setOnItemLongClickListener(mViewFilesOnItemLongClickListener);
        mViewFiles.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mListviewFilesGestureDetector.onTouchEvent(event);
            }
        });

        /*
         * API 13+ does not recognize AbsListView.setAdapter(), so we cast it to
         * explicit class
         */
        if (mViewFiles instanceof ListView)
            ((ListView) mViewFiles).setAdapter(mFileAdapter);
        else
            ((GridView) mViewFiles).setAdapter(mFileAdapter);

        // no comments :-D
        mFooterView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                E.show(FileChooserActivity.this);
                return false;
            }
        });
    }// setupViewFiles()

    /**
     * Setup:<br>
     * - button Cancel;<br>
     * - text field "save as" filename;<br>
     * - button Ok;
     */
    private void setupFooter() {
        // by default, view group footer and all its child views are hidden

        ViewGroup viewGroupFooterContainer = (ViewGroup) findViewById(R.id.afc_filechooser_activity_viewgroup_footer_container);
        ViewGroup viewGroupFooter = (ViewGroup) findViewById(R.id.afc_filechooser_activity_viewgroup_footer);

        if (mIsSaveDialog) {
            viewGroupFooterContainer.setVisibility(View.VISIBLE);
            viewGroupFooter.setVisibility(View.VISIBLE);

            mTxtSaveas.setVisibility(View.VISIBLE);
            mTxtSaveas.setText(getIntent().getStringExtra(_DefaultFilename));
            mTxtSaveas.setOnEditorActionListener(mTxtFilenameOnEditorActionListener);

            mBtnOk.setVisibility(View.VISIBLE);
            mBtnOk.setOnClickListener(mBtnOk_SaveDialog_OnClickListener);
            mBtnOk.setBackgroundResource(R.drawable.afc_selector_button_ok_saveas);

            int size = getResources().getDimensionPixelSize(R.dimen.afc_button_ok_saveas_size);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mBtnOk.getLayoutParams();
            lp.width = size;
            lp.height = size;
            mBtnOk.setLayoutParams(lp);
        }// this is in save mode
        else {
            if (mIsMultiSelection) {
                viewGroupFooterContainer.setVisibility(View.VISIBLE);
                viewGroupFooter.setVisibility(View.VISIBLE);

                ViewGroup.LayoutParams lp = viewGroupFooter.getLayoutParams();
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                viewGroupFooter.setLayoutParams(lp);

                mBtnOk.setMinWidth(getResources().getDimensionPixelSize(R.dimen.afc_single_button_min_width));
                mBtnOk.setText(android.R.string.ok);
                mBtnOk.setVisibility(View.VISIBLE);
                mBtnOk.setOnClickListener(mBtnOk_OpenDialog_OnClickListener);
            }
        }// this is in open mode
    }// setupFooter()

    /**
     * Shows footer view.
     * 
     * @param show
     *            {@code true} or {@code false}.
     * @param text
     *            the message you want to set.
     * @param center
     *            {@code true} or {@code false}.
     */
    private void showFooterView(boolean show, String text, boolean center) {
        if (show) {
            mFooterView.setText(text);

            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            if (!center)
                lp.addRule(RelativeLayout.ABOVE, R.id.afc_filechooser_activity_view_files_footer_view);
            mViewFilesContainer.setLayoutParams(lp);

            lp = (RelativeLayout.LayoutParams) mFooterView.getLayoutParams();
            lp.addRule(RelativeLayout.CENTER_IN_PARENT, center ? 1 : 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, center ? 0 : 1);
            mFooterView.setLayoutParams(lp);

            mFooterView.setVisibility(View.VISIBLE);
        } else
            mFooterView.setVisibility(View.GONE);
    }// showFooterView()

    private void doReloadCurrentLocation() {
        Bundle b = new Bundle();
        b.putString(_Path, mFileAdapter.getPath().toString());
        getSupportLoaderManager().restartLoader(0, b, this);
    }// doReloadCurrentLocation()

    private void doGoHome() {
        // TODO explain why?
        goTo(mRoot);
    }// doGoHome()

    private static final int[] _BtnSortIds = { R.id.afc_settings_sort_view_button_sort_by_name_asc,
            R.id.afc_settings_sort_view_button_sort_by_name_desc, R.id.afc_settings_sort_view_button_sort_by_size_asc,
            R.id.afc_settings_sort_view_button_sort_by_size_desc, R.id.afc_settings_sort_view_button_sort_by_date_asc,
            R.id.afc_settings_sort_view_button_sort_by_date_desc };

    /**
     * Show a dialog for sorting options and resort file list after user
     * selected an option.
     */
    private void doResortViewFiles() {
        final AlertDialog _dialog = Dlg.newDlg(this);

        // get the index of button of current sort type
        int btnCurrentSortTypeIdx = 0;
        switch (DisplayPrefs.getSortType(this)) {
        case BaseFile._SortByName:
            btnCurrentSortTypeIdx = 0;
            break;
        case BaseFile._SortBySize:
            btnCurrentSortTypeIdx = 2;
            break;
        case BaseFile._SortByModificationTime:
            btnCurrentSortTypeIdx = 4;
            break;
        }
        if (!DisplayPrefs.isSortAscending(this))
            btnCurrentSortTypeIdx++;

        View.OnClickListener listener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                _dialog.dismiss();

                Context c = FileChooserActivity.this;

                if (v.getId() == R.id.afc_settings_sort_view_button_sort_by_name_asc) {
                    DisplayPrefs.setSortType(c, BaseFile._SortByName);
                    DisplayPrefs.setSortAscending(c, true);
                } else if (v.getId() == R.id.afc_settings_sort_view_button_sort_by_name_desc) {
                    DisplayPrefs.setSortType(c, BaseFile._SortByName);
                    DisplayPrefs.setSortAscending(c, false);
                } else if (v.getId() == R.id.afc_settings_sort_view_button_sort_by_size_asc) {
                    DisplayPrefs.setSortType(c, BaseFile._SortBySize);
                    DisplayPrefs.setSortAscending(c, true);
                } else if (v.getId() == R.id.afc_settings_sort_view_button_sort_by_size_desc) {
                    DisplayPrefs.setSortType(c, BaseFile._SortBySize);
                    DisplayPrefs.setSortAscending(c, false);
                } else if (v.getId() == R.id.afc_settings_sort_view_button_sort_by_date_asc) {
                    DisplayPrefs.setSortType(c, BaseFile._SortByModificationTime);
                    DisplayPrefs.setSortAscending(c, true);
                } else if (v.getId() == R.id.afc_settings_sort_view_button_sort_by_date_desc) {
                    DisplayPrefs.setSortType(c, BaseFile._SortByModificationTime);
                    DisplayPrefs.setSortAscending(c, false);
                }

                /*
                 * Reload current location.
                 */
                doReloadCurrentLocation();
                supportInvalidateOptionsMenu();
            }// onClick()
        };// listener

        View view = getLayoutInflater().inflate(R.layout.afc_settings_sort_view, null);
        for (int i = 0; i < _BtnSortIds.length; i++) {
            Button btn = (Button) view.findViewById(_BtnSortIds[i]);
            btn.setOnClickListener(listener);
            if (i == btnCurrentSortTypeIdx) {
                btn.setEnabled(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    btn.setText(R.string.afc_ellipsize);
            }
        }

        _dialog.setTitle(R.string.afc_title_sort_by);
        _dialog.setView(view);

        _dialog.show();
    }// doResortViewFiles()

    /**
     * Switch view type between {@link ViewType#List} and {@link ViewType#Grid}
     */
    private void doSwitchViewType() {
        new LoadingDialog(this, R.string.afc_msg_loading, false) {

            @Override
            protected void onPreExecute() {
                // call this first, to let the parent prepare the dialog
                super.onPreExecute();

                switch (DisplayPrefs.getViewType(FileChooserActivity.this)) {
                case Grid:
                    DisplayPrefs.setViewType(FileChooserActivity.this, ViewType.List);
                    break;
                case List:
                    DisplayPrefs.setViewType(FileChooserActivity.this, ViewType.Grid);
                    break;
                }

                setupViewFiles();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    ActivityCompat.invalidateOptionsMenu(FileChooserActivity.this);

                doReloadCurrentLocation();
            }// onPreExecute()

            @Override
            protected Object doInBackground(Void... params) {
                // do nothing :-)
                return null;
            }// doInBackground()
        }.execute();
    }// doSwitchViewType()

    /**
     * Confirms user to create new directory.
     */
    private void doCreateNewDir() {
        if (LocalFileContract._Authority.equals(mFileProviderAuthority)
                && !Utils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Dlg.toast(this, R.string.afc_msg_app_doesnot_have_permission_to_create_files, Dlg._LengthShort);
            return;
        }

        final AlertDialog _dlg = Dlg.newDlg(this);

        View view = getLayoutInflater().inflate(R.layout.afc_simple_text_input_view, null);
        final EditText _textFile = (EditText) view.findViewById(R.id.afc_simple_text_input_view_text1);
        _textFile.setHint(R.string.afc_hint_folder_name);
        _textFile.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Ui.hideSoftKeyboard(FileChooserActivity.this, _textFile.getWindowToken());
                    _dlg.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            }
        });

        _dlg.setView(view);
        _dlg.setTitle(R.string.afc_cmd_new_folder);
        _dlg.setIcon(android.R.drawable.ic_menu_add);
        _dlg.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = _textFile.getText().toString().trim();
                        if (!FileUtils.isFilenameValid(name)) {
                            Dlg.toast(FileChooserActivity.this, getString(R.string.afc_pmsg_filename_is_invalid, name),
                                    Dlg._LengthShort);
                            return;
                        }

                        // TODO
                        // IFile dir = mFileProvider.fromPath(String
                        // .format("%s/%s", getLocation().getAbsolutePath(),
                        // name));
                        // if (dir.mkdir()) {
                        // Dlg.toast(FileChooserActivity.this,
                        // getString(R.string.afc_msg_done), Dlg._LengthShort);
                        // setLocation(getLocation(), null);
                        // } else
                        // Dlg.toast(FileChooserActivity.this,
                        // getString(R.string.afc_pmsg_cannot_create_folder,
                        // name), Dlg._LengthShort);
                    }// onClick()
                });
        _dlg.show();

        final Button _btnOk = _dlg.getButton(DialogInterface.BUTTON_POSITIVE);
        _btnOk.setEnabled(false);

        _textFile.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                _btnOk.setEnabled(FileUtils.isFilenameValid(s.toString().trim()));
            }
        });
    }// doCreateNewDir()

    /**
     * Updates UI that {@code data} will not be deleted.
     * 
     * @param data
     *            {@link IFileDataModel}
     */
    private void notifyDataModelNotDeleted(IFileDataModel data) {
        data.setTobeDeleted(false);
        mFileAdapter.notifyDataSetChanged();
    }// notifyDataModelNotDeleted(()

    /**
     * Deletes a file.
     * 
     * @param file
     *            {@link IFile}
     */
    private void doDeleteFile(final IFileDataModel data) {
        if (LocalFileContract._Authority.equals(mFileProviderAuthority)
                && !Utils.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            notifyDataModelNotDeleted(data);
            Dlg.toast(this, R.string.afc_msg_app_doesnot_have_permission_to_delete_files, Dlg._LengthShort);
            return;
        }

        Dlg.confirmYesno(
                this,
                getString(R.string.afc_pmsg_confirm_delete_file, data.getFile().isFile() ? getString(R.string.afc_file)
                        : getString(R.string.afc_folder), data.getFile().getName()),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new LoadingDialog(FileChooserActivity.this, getString(R.string.afc_pmsg_deleting_file, data
                                .getFile().isFile() ? getString(R.string.afc_file) : getString(R.string.afc_folder),
                                data.getFile().getName()), true) {

                            // private Thread mThread =
                            // FileUtils.createDeleteFileThread(data.getFile(),
                            // mFileProvider,
                            // true);
                            private final boolean _isFile = data.getFile().isFile();

                            private void notifyFileDeleted() {
                                // mFileAdapter.remove(data);
                                mFileAdapter.notifyDataSetChanged();

                                refreshHistories();
                                // TODO remove all duplicate history items

                                Dlg.toast(
                                        FileChooserActivity.this,
                                        getString(
                                                R.string.afc_pmsg_file_has_been_deleted,
                                                _isFile ? getString(R.string.afc_file) : getString(R.string.afc_folder),
                                                data.getFile().getName()), Dlg._LengthShort);
                            }// notifyFileDeleted()

                            @Override
                            protected void onPreExecute() {
                                super.onPreExecute();
                                // mThread.start();
                            }// onPreExecute()

                            @Override
                            protected Object doInBackground(Void... arg0) {
                                // while (mThread.isAlive()) {
                                // try {
                                // mThread.join(DisplayPrefs._DelayTimeWaitingThreads);
                                // } catch (InterruptedException e) {
                                // mThread.interrupt();
                                // }
                                // }
                                return null;
                            }// doInBackground()

                            @Override
                            protected void onCancelled() {
                                // mThread.interrupt();

                                if (data.getFile().exists()) {
                                    notifyDataModelNotDeleted(data);
                                    Dlg.toast(FileChooserActivity.this, R.string.afc_msg_cancelled, Dlg._LengthShort);
                                } else
                                    notifyFileDeleted();

                                super.onCancelled();
                            }// onCancelled()

                            @Override
                            protected void onPostExecute(Object result) {
                                super.onPostExecute(result);

                                if (data.getFile().exists()) {
                                    notifyDataModelNotDeleted(data);
                                    Dlg.toast(
                                            FileChooserActivity.this,
                                            getString(R.string.afc_pmsg_cannot_delete_file,
                                                    data.getFile().isFile() ? getString(R.string.afc_file)
                                                            : getString(R.string.afc_folder), data.getFile().getName()),
                                            Dlg._LengthShort);
                                } else
                                    notifyFileDeleted();
                            }// onPostExecute()
                        }.execute();// LoadingDialog
                    }// onClick()
                }, new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        notifyDataModelNotDeleted(data);
                    }// onCancel()
                });
    }// doDeleteFile()

    /**
     * As the name means.
     * 
     * @param filename
     * @since v1.91
     */
    private void doCheckSaveasFilenameAndFinish(String filename) {
        if (filename.length() == 0) {
            Dlg.toast(this, R.string.afc_msg_filename_is_empty, Dlg._LengthShort);
        } else {
            // final IFile _file =
            // mFileProvider.fromPath(getLocation().getAbsolutePath() +
            // File.separator + filename);
            //
            // if (!FileUtils.isFilenameValid(filename)) {
            // Dlg.toast(this, getString(R.string.afc_pmsg_filename_is_invalid,
            // filename), Dlg._LengthShort);
            // } else if (_file.isFile()) {
            // Dlg.confirmYesno(FileChooserActivity.this,
            // getString(R.string.afc_pmsg_confirm_replace_file,
            // _file.getName()),
            // new DialogInterface.OnClickListener() {
            //
            // @Override
            // public void onClick(DialogInterface dialog, int which) {
            // doFinish(_file);
            // }
            // });
            // } else if (_file.isDirectory()) {
            // Dlg.toast(this,
            // getString(R.string.afc_pmsg_filename_is_directory,
            // _file.getName()), Dlg._LengthShort);
            // } else
            // doFinish(_file);
        }
    }// doCheckSaveasFilenameAndFinish()

    /**
     * Sets current location.
     * 
     * @param path
     *            the path.
     * @param listener
     *            {@link TaskListener}: the second parameter {@code any} in
     *            {@link TaskListener#onFinish(boolean, Object)} will be
     *            {@code path}.
     */
    private void setLocation(final Uri path, final TaskListener listener) {
        setLocation(path, listener, null);
    }// setLocation()

    /**
     * Sets current location.
     * 
     * @param path
     *            the path.
     * @param listener
     *            {@link TaskListener}: the second parameter {@code any} in
     *            {@link TaskListener#onFinish(boolean, Object)} will be
     *            {@code path}.
     * @param selectedFile
     *            the file should be selected after loading location done. Can
     *            be {@code null}.
     */
    private void setLocation(final Uri path, final TaskListener listener, final Uri selectedFile) {
        if (BuildConfig.DEBUG)
            Log.d(_ClassName, "setLocation() >> path = " + path);

        new LoadingDialog(this, R.string.afc_msg_loading, true) {

            Cursor mCursor;
            boolean hasMoreFiles = false;
            int shouldBeSelectedIdx = -1;

            @Override
            protected Object doInBackground(Void... params) {
                try {
                    // TODO: task ID? path == null?
                    mCursor = getContentResolver().query(
                            BaseFile.genContentUriBase(mFileProviderAuthority).buildUpon()
                                    .appendPath(String.valueOf(path)).build(), null, null, null, null);

                    if (mCursor != null && !isCancelled()) {
                        // TODO
                        // if (selectedFile != null && selectedFile.exists()
                        // && selectedFile.parentFile().equalsToPath(path)) {
                        // for (int i = 0; i < mFiles.size(); i++) {
                        // if (mFiles.get(i).equalsToPath(selectedFile)) {
                        // shouldBeSelectedIdx = i;
                        // break;
                        // }
                        // }
                        // } else if (mLastPath != null && mLastPath.length() >=
                        // path.getAbsolutePath().length()) {
                        // for (int i = 0; i < mFiles.size(); i++) {
                        // IFile f = mFiles.get(i);
                        // if (f.isDirectory() &&
                        // mLastPath.startsWith(f.getAbsolutePath())) {
                        // shouldBeSelectedIdx = i;
                        // break;
                        // }
                        // }
                        // }
                    }// if mFiles != null
                } catch (Throwable t) {
                    Log.e(_ClassName, t.toString());
                    t.printStackTrace();
                    setLastException(t);
                    cancel(false);
                }
                return null;
            }// doInBackground()

            @Override
            protected void onCancelled() {
                // TODO: task ID?
                getContentResolver().query(
                        BaseFile.genContentUriBase(mFileProviderAuthority).buildUpon().appendPath(String.valueOf(path))
                                .appendQueryParameter(BaseFile._ParamCancel, "1").build(), null, null, null, null);

                super.onCancelled();
                Dlg.toast(FileChooserActivity.this, R.string.afc_msg_cancelled, Dlg._LengthShort);
            }// onCancelled()

            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);

                if (mCursor == null) {
                    Dlg.toast(
                            FileChooserActivity.this,
                            getString(R.string.afc_pmsg_cannot_access_dir, path != null ? path.getLastPathSegment()
                                    : ""), Dlg._LengthShort);
                    if (listener != null)
                        listener.onFinish(false, path);
                    return;
                }

                // update list view

                mFileAdapter.changeCursor(mCursor);

                // update footers

                mFooterView.setVisibility(hasMoreFiles || mFileAdapter.isEmpty() ? View.VISIBLE : View.GONE);
                if (hasMoreFiles)
                    mFooterView.setText(getString(R.string.afc_pmsg_max_file_count_allowed, mMaxFileCount));
                else if (mFileAdapter.isEmpty())
                    mFooterView.setText(R.string.afc_msg_empty);

                /*
                 * We use a Runnable to make sure this work. Because if the list
                 * view is handling data, this might not work.
                 */
                mViewFiles.post(new Runnable() {

                    @Override
                    public void run() {
                        if (shouldBeSelectedIdx >= 0 && shouldBeSelectedIdx < mFileAdapter.getCount()) {
                            mViewFiles.setSelection(shouldBeSelectedIdx);
                        } else if (!mFileAdapter.isEmpty())
                            mViewFiles.setSelection(0);
                    }// run()
                });

                /*
                 * navigation buttons
                 */
                // createLocationButtons(mCursor.getNotificationUri());

                if (listener != null)
                    listener.onFinish(true, path);
            }// onPostExecute()
        }.execute();// new LoadingDialog()
    }// setLocation()

    /**
     * Goes to a specified location.
     * 
     * @param dir
     *            a directory, of course.
     * @return {@code true} if {@code dir} <b><i>can</i></b> be browsed to.
     * @since v4.3 beta
     */
    private boolean goTo(final Uri dir) {
        Bundle b = new Bundle();
        b.putString(_Path, mFileAdapter.getPath().toString());
        getSupportLoaderManager().restartLoader(0, b, this);
        // TODO history?
        // // mHistory.truncateAfter(mLastPath);
        // // mHistory.push(dir);
        // });
        return true;
    }// goTo()

    private void createLocationButtons(Uri path) {
        mViewLocations.removeAllViews();

        LinearLayout.LayoutParams lpBtnLoc = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lpBtnLoc.gravity = Gravity.CENTER;
        LinearLayout.LayoutParams lpDivider = null;
        LayoutInflater inflater = getLayoutInflater();
        final int _dim = getResources().getDimensionPixelSize(R.dimen.afc_5dp);
        int count = 0;
        for (String pathName : path.getPathSegments()) {
            TextView btnLoc = (TextView) inflater.inflate(R.layout.afc_button_location, null);
            btnLoc.setText(pathName != null ? pathName : getString(R.string.afc_root));
            btnLoc.setTag(path);
            btnLoc.setOnClickListener(mBtnLocationOnClickListener);
            btnLoc.setOnLongClickListener(mBtnLocationOnLongClickListener);
            mViewLocations.addView(btnLoc, 0, lpBtnLoc);

            if (count++ == 0) {
                Rect r = new Rect();
                btnLoc.getPaint().getTextBounds(path.getLastPathSegment(), 0, path.getLastPathSegment().length(), r);
                if (r.width() >= getResources().getDimensionPixelSize(R.dimen.afc_button_location_max_width)
                        - btnLoc.getPaddingLeft() - btnLoc.getPaddingRight()) {
                    mTxtFullDirName.setText(path.getLastPathSegment());
                    mTxtFullDirName.setVisibility(View.VISIBLE);
                } else
                    mTxtFullDirName.setVisibility(View.GONE);
            }

            if (path != null) {
                View divider = inflater.inflate(R.layout.afc_view_locations_divider, null);

                if (lpDivider == null) {
                    lpDivider = new LinearLayout.LayoutParams(_dim, _dim);
                    lpDivider.gravity = Gravity.CENTER;
                    lpDivider.setMargins(_dim, _dim, _dim, _dim);
                }
                mViewLocations.addView(divider, 0, lpDivider);
            }
        }

        mViewLocationsContainer.post(new Runnable() {

            public void run() {
                mViewLocationsContainer.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        });
    }// createLocationButtons()

    /**
     * Refreshes all the histories. This removes invalid items (which are not
     * existed anymore).
     */
    private void refreshHistories() {
        HistoryFilter<Uri> historyFilter = new HistoryFilter<Uri>() {

            @Override
            public boolean accept(Uri item) {
                return !BaseFileProviderUtils.isDirectory(FileChooserActivity.this, mFileProviderAuthority, item);
            }
        };

        mHistory.removeAll(historyFilter);
    }// refreshHistories()

    /**
     * Finishes this activity.
     * 
     * @param files
     *            list of {@link IFile}
     */
    private void doFinish(IFile... files) {
        List<IFile> list = new ArrayList<IFile>();
        for (IFile f : files)
            list.add(f);
        doFinish((ArrayList<IFile>) list);
    }

    /**
     * Finishes this activity.
     * 
     * @param files
     *            list of {@link IFile}
     */
    private void doFinish(ArrayList<IFile> files) {
        if (files == null || files.isEmpty()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Intent intent = new Intent();

        // set results
        intent.putExtra(_Results, files);

        // return flags for further use (in case the caller needs)
        intent.putExtra(_FilterMode, mFilterMode);
        intent.putExtra(_SaveDialog, mIsSaveDialog);

        setResult(RESULT_OK, intent);

        // TODO
        // if (DisplayPrefs.isRememberLastLocation(this) && getLocation() !=
        // null) {
        // DisplayPrefs.setLastLocation(this, getLocation().getAbsolutePath());
        // } else
        // DisplayPrefs.setLastLocation(this, null);

        finish();
    }// doFinish()

    /**********************************************************
     * BUTTON LISTENERS
     */

    private final View.OnClickListener mBtnGoBackOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            /*
             * if user deleted a dir which was one in history, then maybe there
             * are duplicates, so we check and remove them here
             */
            Uri currentLoc = mFileAdapter.getPath();
            Uri preLoc = null;
            // TODO
            // while (currentLoc.equalsToPath(preLoc =
            // mHistory.prevOf(currentLoc)))
            // mHistory.remove(preLoc);

            if (preLoc != null) {
                // TODO
                // setLocation(preLoc, new TaskListener() {
                //
                // @Override
                // public void onFinish(boolean ok, Object any) {
                // if (ok) {
                // mViewGoBack.setEnabled(mHistory.prevOf(getLocation()) !=
                // null);
                // mViewGoForward.setEnabled(true);
                // }
                // }
                // });
            } else {
                mViewGoBack.setEnabled(false);
            }
        }
    };// mBtnGoBackOnClickListener

    private final View.OnClickListener mBtnLocationOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getTag() instanceof IFile)
                goTo((Uri) v.getTag());
        }
    };// mBtnLocationOnClickListener

    private final View.OnLongClickListener mBtnLocationOnLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            if (BaseFile._FilterFilesOnly == mFilterMode || mIsSaveDialog)
                return false;

            doFinish((IFile) v.getTag());

            return false;
        }

    };// mBtnLocationOnLongClickListener

    private final View.OnClickListener mBtnGoForwardOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            /*
             * if user deleted a dir which was one in history, then maybe there
             * are duplicates, so we check and remove them here
             */
            Uri currentLoc = mFileAdapter.getPath();
            Uri nextLoc = null;
            // TODO
            // while (currentLoc.equalsToPath(nextLoc =
            // mHistory.nextOf(currentLoc)))
            // mHistory.remove(nextLoc);

            if (nextLoc != null) {
                // TODO
                // setLocation(nextLoc, new TaskListener() {
                //
                // @Override
                // public void onFinish(boolean ok, Object any) {
                // if (ok) {
                // mViewGoBack.setEnabled(true);
                // mViewGoForward.setEnabled(mHistory.nextOf(getLocation()) !=
                // null);
                // }
                // }
                // });
            } else {
                mViewGoForward.setEnabled(false);
            }
        }
    };// mBtnGoForwardOnClickListener

    private final View.OnLongClickListener mBtnGoBackForwardOnLongClickListener = new View.OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            // TODO
            // ViewFilesContextMenuUtils.doShowHistoryContents(FileChooserActivity.this,
            // mFileProvider, mFullHistory,
            // getLocation(), new TaskListener() {
            //
            // @Override
            // public void onFinish(boolean ok, Object any) {
            // mHistory.removeAll(new HistoryFilter<IFile>() {
            //
            // @Override
            // public boolean accept(IFile item) {
            // return mFullHistory.indexOf(item) < 0;
            // }
            // });
            //
            // if (any instanceof IFile) {
            // setLocation((IFile) any, new TaskListener() {
            //
            // @Override
            // public void onFinish(boolean ok, Object any) {
            // if (ok)
            // mHistory.notifyHistoryChanged();
            // }
            // });
            // } else if (mHistory.isEmpty()) {
            // mHistory.push(getLocation());
            // }
            // }// onFinish()
            // });
            return false;
        }// onLongClick()
    };// mBtnGoBackForwardOnLongClickListener

    private final TextView.OnEditorActionListener mTxtFilenameOnEditorActionListener = new TextView.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                Ui.hideSoftKeyboard(FileChooserActivity.this, mTxtSaveas.getWindowToken());
                mBtnOk.performClick();
                return true;
            }
            return false;
        }
    };// mTxtFilenameOnEditorActionListener

    private final View.OnClickListener mBtnOk_SaveDialog_OnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            Ui.hideSoftKeyboard(FileChooserActivity.this, mTxtSaveas.getWindowToken());
            String filename = mTxtSaveas.getText().toString().trim();
            doCheckSaveasFilenameAndFinish(filename);
        }
    };// mBtnOk_SaveDialog_OnClickListener

    private final View.OnClickListener mBtnOk_OpenDialog_OnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            List<IFile> list = new ArrayList<IFile>();
            for (int i = 0; i < mViewFiles.getAdapter().getCount(); i++) {
                // NOTE: header and footer don't have data
                Object obj = mViewFiles.getAdapter().getItem(i);
                if (obj instanceof IFileDataModel) {
                    IFileDataModel dm = (IFileDataModel) obj;
                    if (dm.isSelected())
                        list.add(dm.getFile());
                }
            }
            doFinish((ArrayList<IFile>) list);
        }
    };// mBtnOk_OpenDialog_OnClickListener

    /*
     * LIST VIEW HELPER
     */

    private GestureDetector mListviewFilesGestureDetector;

    private void initGestureDetector() {
        mListviewFilesGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

            private Object getData(float x, float y) {
                int i = getSubViewId(x, y);
                if (i >= 0)
                    return mViewFiles.getItemAtPosition(mViewFiles.getFirstVisiblePosition() + i);
                return null;
            }// getSubView()

            /**
             * Gets {@link IFileDataModel} from {@code e}.
             * 
             * @param e
             *            {@link MotionEvent}.
             * @return the data model, or {@code null} if not available.
             */
            private IFileDataModel getDataModel(MotionEvent e) {
                Object o = getData(e.getX(), e.getY());
                return o instanceof IFileDataModel ? (IFileDataModel) o : null;
            }// getDataModel()

            private int getSubViewId(float x, float y) {
                Rect r = new Rect();
                for (int i = 0; i < mViewFiles.getChildCount(); i++) {
                    mViewFiles.getChildAt(i).getHitRect(r);
                    if (r.contains((int) x, (int) y))
                        return i;
                }

                return -1;
            }// getSubViewId()

            @Override
            public void onLongPress(MotionEvent e) {
                // do nothing
            }// onLongPress()

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // do nothing
                return false;
            }// onSingleTapConfirmed()

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (mDoubleTapToChooseFiles) {
                    if (mIsMultiSelection)
                        return false;

                    IFileDataModel data = getDataModel(e);
                    if (data == null)
                        return false;

                    // TODO
                    // if (data.getFile().isDirectory()
                    // &&
                    // IFileProvider.FilterMode.FilesOnly.equals(mFileProvider.getFilterMode()))
                    // return false;

                    /*
                     * If mFilterMode == DirectoriesOnly, files won't be shown.
                     */

                    if (mIsSaveDialog) {
                        if (data.getFile().isFile()) {
                            mTxtSaveas.setText(data.getFile().getName());
                            doCheckSaveasFilenameAndFinish(data.getFile().getName());
                        } else
                            return false;
                    } else
                        doFinish(data.getFile());
                }// double tap to choose files
                else {
                    // do nothing
                    return false;
                }// single tap to choose files

                return true;
            }// onDoubleTap()

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                /*
                 * Sometimes e1 or e2 can be null. This came from users'
                 * experiences.
                 */
                if (e1 == null || e2 == null)
                    return false;

                final int _max_y_distance = 19;// 10 is too short :-D
                final int _min_x_distance = 80;
                final int _min_x_velocity = 200;
                if (Math.abs(e1.getY() - e2.getY()) < _max_y_distance
                        && Math.abs(e1.getX() - e2.getX()) > _min_x_distance && Math.abs(velocityX) > _min_x_velocity) {
                    Object o = getData(e1.getX(), e1.getY());
                    if (o instanceof IFileDataModel) {
                        ((IFileDataModel) o).setTobeDeleted(true);
                        mFileAdapter.notifyDataSetChanged();
                        doDeleteFile((IFileDataModel) o);
                    }
                }

                /*
                 * Always return false to let the default handler draw the item
                 * properly.
                 */
                return false;
            }// onFling()
        });// mListviewFilesGestureDetector
    }// initGestureDetector()

    private final AdapterView.OnItemClickListener mViewFilesOnItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // TODO
            // IFileDataModel data = mFileAdapter.getItem(position);
            //
            // if (data.getFile().isDirectory()) {
            // goTo(data.getFile());
            // return;
            // }
            //
            // if (mIsSaveDialog)
            // mTxtSaveas.setText(data.getFile().getName());
            //
            // if (mDoubleTapToChooseFiles) {
            // // do nothing
            // return;
            // }// double tap to choose files
            // else {
            // if (mIsMultiSelection)
            // return;
            //
            // if (mIsSaveDialog)
            // doCheckSaveasFilenameAndFinish(data.getFile().getName());
            // else
            // doFinish(data.getFile());
            // }// single tap to choose files
        }// onItemClick()
    };// mViewFilesOnItemClickListener

    private final AdapterView.OnItemLongClickListener mViewFilesOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            // TODO
            // IFileDataModel data = mFileAdapter.getItem(position);
            //
            // if (mDoubleTapToChooseFiles) {
            // // do nothing
            // }// double tap to choose files
            // else {
            // if (!mIsSaveDialog
            // && !mIsMultiSelection
            // && data.getFile().isDirectory()
            // &&
            // (IFileProvider.FilterMode.DirectoriesOnly.equals(mFileProvider.getFilterMode())
            // || IFileProvider.FilterMode.FilesAndDirectories
            // .equals(mFileProvider.getFilterMode()))) {
            // doFinish(data.getFile());
            // }
            // }// single tap to choose files

            // notify that we already handled long click here
            return true;
        }// onItemLongClick()
    };// mViewFilesOnItemLongClickListener
}