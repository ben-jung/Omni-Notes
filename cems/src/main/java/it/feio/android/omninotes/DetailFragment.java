/*
 * Copyright (C) 2018 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.feio.android.omninotes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.epson.epos2.Epos2Exception;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.neopixl.pixlui.components.edittext.EditText;
import com.neopixl.pixlui.components.textview.TextView;
import com.pushbullet.android.extension.MessagingExtension;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Style;
import it.feio.android.checklistview.exceptions.ViewNotSupportedException;
import it.feio.android.checklistview.interfaces.CheckListChangedListener;
import it.feio.android.checklistview.models.CheckListView;
import it.feio.android.checklistview.models.CheckListViewItem;
import it.feio.android.checklistview.models.ChecklistManager;
import it.feio.android.omninotes.async.AttachmentTask;
import it.feio.android.omninotes.async.bus.CategoriesUpdatedEvent;
import it.feio.android.omninotes.async.bus.NotesUpdatedEvent;
import it.feio.android.omninotes.async.bus.PushbulletReplyEvent;
import it.feio.android.omninotes.async.bus.SwitchFragmentEvent;
import it.feio.android.omninotes.async.notes.NoteProcessorDelete;
import it.feio.android.omninotes.async.notes.SaveNoteTask;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.helpers.AttachmentsHelper;
import it.feio.android.omninotes.helpers.PermissionsHelper;
import it.feio.android.omninotes.helpers.date.DateHelper;
import it.feio.android.omninotes.models.*;
import it.feio.android.omninotes.models.adapters.AttachmentAdapter;
import it.feio.android.omninotes.models.adapters.NavDrawerCategoryAdapter;
import it.feio.android.omninotes.models.adapters.PlacesAutoCompleteAdapter;
import it.feio.android.omninotes.models.listeners.OnAttachingFileListener;
import it.feio.android.omninotes.models.listeners.OnGeoUtilResultListener;
import it.feio.android.omninotes.models.listeners.OnNoteSaved;
import it.feio.android.omninotes.models.listeners.OnReminderPickedListener;
import it.feio.android.omninotes.models.views.ExpandableHeightGridView;
import it.feio.android.omninotes.network.CemsApi;
import it.feio.android.omninotes.printer.ShowMsg;
import it.feio.android.omninotes.utils.*;
import it.feio.android.omninotes.utils.Display;
import it.feio.android.omninotes.utils.date.DateUtils;
import it.feio.android.omninotes.utils.date.ReminderPickers;
import it.feio.android.pixlui.links.TextLinkClickListener;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;


public class DetailFragment extends BaseFragment implements OnTouchListener, ReceiveListener,
		OnAttachingFileListener, TextWatcher, CheckListChangedListener, OnNoteSaved,
		OnGeoUtilResultListener {

	private static final int TAKE_PHOTO = 1;
	private static final int TAKE_VIDEO = 2;
	private static final int SET_PASSWORD = 3;
	private static final int SKETCH = 4;
	private static final int CATEGORY = 5;
	private static final int DETAIL = 6;
	private static final int FILES = 7;
	private static final int RC_READ_EXTERNAL_STORAGE_PERMISSION = 1;

	private static final String API_BASE_URL = "http://192.168.0.9:8000/catalog/";
	private CemsApi cemsApi;

	@BindView(R.id.detail_root)
	ViewGroup root;
	@BindView(R.id.detail_title)
	EditText title;
	@BindView(R.id.detail_content)
	EditText content;
	@BindView(R.id.detail_attachments_above)
	ViewStub attachmentsAbove;
	@BindView(R.id.detail_attachments_below)
	ViewStub attachmentsBelow;
	@Nullable
	@BindView(R.id.gridview)
	ExpandableHeightGridView mGridView;
	@BindView(R.id.location)
	TextView locationTextView;
	@BindView(R.id.detail_timestamps)
	View timestampsView;
	@BindView(R.id.detail_tile_card)
	View titleCardView;
	@BindView(R.id.content_wrapper)
	ScrollView scrollView;
	@BindView(R.id.creation)
	TextView creationTextView;
	@BindView(R.id.last_modification)
	TextView lastModificationTextView;
	@BindView(R.id.title_wrapper)
	View titleWrapperView;
	@BindView(R.id.tag_marker)
	View tagMarkerView;
	@BindView(R.id.detail_wrapper)
	ViewManager detailWrapperView;
	@BindView(R.id.snackbar_placeholder)
	View snackBarPlaceholder;

	public OnDateSetListener onDateSetListener;
	public OnTimeSetListener onTimeSetListener;
	public boolean goBack = false;
	View toggleChecklistView;
	private Uri attachmentUri;
	private AttachmentAdapter mAttachmentAdapter;
	private MaterialDialog attachmentDialog;
	private Note note;
	private Note noteTmp;
	private Note noteOriginal;
	// Audio recording
	private String recordName;
	private MediaRecorder mRecorder = null;
	private MediaPlayer mPlayer = null;
	private boolean isRecording = false;
	private View isPlayingView = null;
	private Bitmap recordingBitmap;
	private ChecklistManager mChecklistManager;
	// Values to print result
	private String exitMessage;
	private Style exitCroutonStyle = ONStyle.CONFIRM;
	// Flag to check if after editing it will return to ListActivity or not
	// and in the last case a Toast will be shown instead than Crouton
	private boolean afterSavedReturnsToList = true;
	private boolean showKeyboard = false;
	private boolean swiping;
	private int startSwipeX;
	private SharedPreferences prefs;
	private boolean orientationChanged;
	private long audioRecordingTimeStart;
	private long audioRecordingTime;
	private DetailFragment mFragment;
	private Attachment sketchEdited;
	private int contentLineCounter = 1;
	private int contentCursorPosition;
	private ArrayList<String> mergedNotesIds;
	private MainActivity mainActivity;
	private boolean activityPausing;

	// Printer
	private Printer mPrinter = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFragment = this;
		buildNetworkService();
	}

	public void buildNetworkService(){
		if (cemsApi == null){
			Retrofit retrofit = new Retrofit.Builder()
					.baseUrl(API_BASE_URL)
					.addConverterFactory(GsonConverterFactory.create())
					.build();

			cemsApi = retrofit.create(CemsApi.class);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		EventBus.getDefault().post(new SwitchFragmentEvent(SwitchFragmentEvent.Direction.CHILDREN));
		EventBus.getDefault().register(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
		GeocodeHelper.stop();
	}

	@Override
	public void onResume() {
		super.onResume();
		activityPausing = false;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_detail, container, false);
		ButterKnife.bind(this, view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mainActivity = (MainActivity) getActivity();

		prefs = mainActivity.prefs;

		mainActivity.getSupportActionBar().setDisplayShowTitleEnabled(false);
		mainActivity.getToolbar().setNavigationOnClickListener(v -> navigateUp());

		// Force the navigation drawer to stay opened if tablet mode is on, otherwise has to stay closed
		if (NavigationDrawerFragment.isDoublePanelActive()) {
			mainActivity.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
		} else {
			mainActivity.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
		}

		// Restored temp note after orientation change
		if (savedInstanceState != null) {
			noteTmp = savedInstanceState.getParcelable("noteTmp");
			note = savedInstanceState.getParcelable("note");
			noteOriginal = savedInstanceState.getParcelable("noteOriginal");
			attachmentUri = savedInstanceState.getParcelable("attachmentUri");
			orientationChanged = savedInstanceState.getBoolean("orientationChanged");
		}

		// Added the sketched image if present returning from SketchFragment
		if (mainActivity.sketchUri != null) {
			Attachment mAttachment = new Attachment(mainActivity.sketchUri, Constants.MIME_TYPE_SKETCH);
			addAttachment(mAttachment);
			mainActivity.sketchUri = null;
			// Removes previous version of edited image
			if (sketchEdited != null) {
				noteTmp.getAttachmentsList().remove(sketchEdited);
				sketchEdited = null;
			}
		}

		init();

		setHasOptionsMenu(true);
		setRetainInstance(false);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (noteTmp != null) {
			noteTmp.setTitle(getNoteTitle());
			noteTmp.setContent(getNoteContent());
			outState.putParcelable("noteTmp", noteTmp);
			outState.putParcelable("note", note);
			outState.putParcelable("noteOriginal", noteOriginal);
			outState.putParcelable("attachmentUri", attachmentUri);
			outState.putBoolean("orientationChanged", orientationChanged);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPause() {
		super.onPause();

		activityPausing = true;

		// Checks "goBack" value to avoid performing a double saving
		if (!goBack) {
			saveNote(this);
		}

		if (toggleChecklistView != null) {
			KeyboardUtils.hideKeyboard(toggleChecklistView);
			content.clearFocus();
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (getResources().getConfiguration().orientation != newConfig.orientation) {
			orientationChanged = true;
		}
	}

	private void init() {

		// Handling of Intent actions
		handleIntents();

		if (noteOriginal == null) {
			noteOriginal = getArguments().getParcelable(Constants.INTENT_NOTE);
		}

		if (note == null) {
			note = new Note(noteOriginal);
		}

		if (noteTmp == null) {
			noteTmp = new Note(note);
		}

		if (noteTmp.isLocked() && !noteTmp.isPasswordChecked()) {
			checkNoteLock(noteTmp);
			return;
		}

		initViews();
	}

	/**
	 * Checks note lock and password before showing note content
	 */
	private void checkNoteLock(Note note) {
		// If note is locked security password will be requested
		if (note.isLocked()
				&& prefs.getString(Constants.PREF_PASSWORD, null) != null
				&& !prefs.getBoolean("settings_password_access", false)) {
			PasswordHelper.requestPassword(mainActivity, passwordConfirmed -> {
				if (passwordConfirmed) {
					noteTmp.setPasswordChecked(true);
					init();
				} else {
					goBack = true;
					goHome();
				}
			});
		} else {
			noteTmp.setPasswordChecked(true);
			init();
		}
	}

	private void handleIntents() {
		Intent i = mainActivity.getIntent();

		if (IntentChecker.checkAction(i, Constants.ACTION_MERGE)) {
			noteOriginal = new Note();
			note = new Note(noteOriginal);
			noteTmp = getArguments().getParcelable(Constants.INTENT_NOTE);
			if (i.getStringArrayListExtra("merged_notes") != null) {
				mergedNotesIds = i.getStringArrayListExtra("merged_notes");
			}
		}

		// Action called from home shortcut
		if (IntentChecker.checkAction(i, Constants.ACTION_SHORTCUT, Constants.ACTION_NOTIFICATION_CLICK)) {
			afterSavedReturnsToList = false;
			noteOriginal = DbHelper.getInstance().getNote(i.getLongExtra(Constants.INTENT_KEY, 0));
			// Checks if the note pointed from the shortcut has been deleted
			try {
				note = new Note(noteOriginal);
				noteTmp = new Note(noteOriginal);
			} catch (NullPointerException e) {
				mainActivity.showToast(getText(R.string.shortcut_note_deleted), Toast.LENGTH_LONG);
				mainActivity.finish();
			}
		}

		// Check if is launched from a widget
		if (IntentChecker.checkAction(i, Constants.ACTION_WIDGET, Constants.ACTION_WIDGET_TAKE_PHOTO)) {

			afterSavedReturnsToList = false;
			showKeyboard = true;

			//  with tags to set tag
			if (i.hasExtra(Constants.INTENT_WIDGET)) {
				String widgetId = i.getExtras().get(Constants.INTENT_WIDGET).toString();
				if (widgetId != null) {
					String sqlCondition = prefs.getString(Constants.PREF_WIDGET_PREFIX + widgetId, "");
					String categoryId = TextHelper.checkIntentCategory(sqlCondition);
					if (categoryId != null) {
						Category category;
						try {
							category = DbHelper.getInstance().getCategory(parseLong(categoryId));
							noteTmp = new Note();
							noteTmp.setCategory(category);
						} catch (NumberFormatException e) {
							Log.e(Constants.TAG, "Category with not-numeric value!", e);
						}
					}
				}
			}

			// Sub-action is to take a photo
			if (IntentChecker.checkAction(i, Constants.ACTION_WIDGET_TAKE_PHOTO)) {
				takePhoto();
			}
		}

		if (IntentChecker.checkAction(i, Constants.ACTION_FAB_TAKE_PHOTO)) {
			takePhoto();
		}

		// Handles third party apps requests of sharing
		if (IntentChecker.checkAction(i, Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE, Constants.INTENT_GOOGLE_NOW)
				&& i.getType() != null) {

			afterSavedReturnsToList = false;

			if (noteTmp == null) noteTmp = new Note();

			// Text title
			String title = i.getStringExtra(Intent.EXTRA_SUBJECT);
			if (title != null) {
				noteTmp.setTitle(title);
			}

			// Text content
			String content = i.getStringExtra(Intent.EXTRA_TEXT);
			if (content != null) {
				noteTmp.setContent(content);
			}

			// Single attachment data
			Uri uri = i.getParcelableExtra(Intent.EXTRA_STREAM);
			// Due to the fact that Google Now passes intent as text but with
			// audio recording attached the case must be handled in specific way
			if (uri != null && !Constants.INTENT_GOOGLE_NOW.equals(i.getAction())) {
				String name = FileHelper.getNameFromUri(mainActivity, uri);
				new AttachmentTask(this, uri, name, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			}

			// Multiple attachment data
			ArrayList<Uri> uris = i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			if (uris != null) {
				for (Uri uriSingle : uris) {
					String name = FileHelper.getNameFromUri(mainActivity, uriSingle);
					new AttachmentTask(this, uriSingle, name, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
			}
		}

		if (IntentChecker.checkAction(i, Intent.ACTION_MAIN, Constants.ACTION_WIDGET_SHOW_LIST, Constants
				.ACTION_SHORTCUT_WIDGET, Constants.ACTION_WIDGET)) {
			showKeyboard = true;
		}

		i.setAction(null);
	}

	@SuppressLint("NewApi")
	private void initViews() {

		// Sets onTouchListener to the whole activity to swipe notes
		root.setOnTouchListener(this);

		// Overrides font sizes with the one selected from user
		Fonts.overrideTextSize(mainActivity, prefs, root);

		// Color of tag marker if note is tagged a function is active in preferences
		setTagMarkerColor(noteTmp.getCategory());

		initViewTitle();

		initViewContent();

		initViewLocation();

		initViewAttachments();

		initViewFooter();
	}

	private void initViewFooter() {
		// Footer dates of creation...
		String creation = DateHelper.getFormattedDate(noteTmp.getCreation(), prefs.getBoolean(Constants
				.PREF_PRETTIFIED_DATES, true));
		creationTextView.append(creation.length() > 0 ? getString(R.string.creation) + " " + creation : "");
		if (creationTextView.getText().length() == 0)
			creationTextView.setVisibility(View.GONE);

		// ... and last modification
		String lastModification = DateHelper.getFormattedDate(noteTmp.getLastModification(), prefs.getBoolean(Constants
				.PREF_PRETTIFIED_DATES, true));
		lastModificationTextView.append(lastModification.length() > 0 ? getString(R.string.last_update) + " " +
				lastModification : "");
		if (lastModificationTextView.getText().length() == 0)
			lastModificationTextView.setVisibility(View.GONE);
	}

	private void initViewLocation() {

		DetailFragment detailFragment = this;

		if (isNoteLocationValid()) {
			if (TextUtils.isEmpty(noteTmp.getAddress())) {
				//FIXME: What's this "sasd"?
				GeocodeHelper.getAddressFromCoordinates(new Location("sasd"), detailFragment);
			} else {
				locationTextView.setText(noteTmp.getAddress());
				locationTextView.setVisibility(View.VISIBLE);
			}
		}

		// Automatic location insertion
		if (prefs.getBoolean(Constants.PREF_AUTO_LOCATION, false) && noteTmp.get_id() == null) {
			getLocation(detailFragment);
		}

		locationTextView.setOnClickListener(v -> {
			String uriString = "geo:" + noteTmp.getLatitude() + ',' + noteTmp.getLongitude()
					+ "?q=" + noteTmp.getLatitude() + ',' + noteTmp.getLongitude();
			Intent locationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
			if (!IntentChecker.isAvailable(mainActivity, locationIntent, null)) {
				uriString = "http://maps.google.com/maps?q=" + noteTmp.getLatitude() + ',' + noteTmp
						.getLongitude();
				locationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
			}
			startActivity(locationIntent);
		});
		locationTextView.setOnLongClickListener(v -> {
			MaterialDialog.Builder builder = new MaterialDialog.Builder(mainActivity);
			builder.content(R.string.remove_location);
			builder.positiveText(R.string.ok);
			builder.callback(new MaterialDialog.ButtonCallback() {
				@Override
				public void onPositive(MaterialDialog materialDialog) {
					noteTmp.setLatitude("");
					noteTmp.setLongitude("");
					fade(locationTextView, false);
				}
			});
			MaterialDialog dialog = builder.build();
			dialog.show();
			return true;
		});
	}

	private void getLocation(OnGeoUtilResultListener onGeoUtilResultListener) {
		PermissionsHelper.requestPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION, R.string
				.permission_coarse_location, snackBarPlaceholder, () -> GeocodeHelper.getLocation
				(onGeoUtilResultListener));
	}

	/**
	 * Performs an action when long-click option is selected
	 *
	 * @param attachmentPosition
	 * @param i                  item index
	 */
	private void performAttachmentAction(int attachmentPosition, int i) {
		switch (getResources().getStringArray(R.array.attachments_actions_values)[i]) {
			case "share":
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				Attachment attachment = mAttachmentAdapter.getItem(attachmentPosition);
				shareIntent.setType(StorageHelper.getMimeType(OmniNotes.getAppContext(), attachment.getUri()));
				shareIntent.putExtra(Intent.EXTRA_STREAM, attachment.getUri());
				if (IntentChecker.isAvailable(OmniNotes.getAppContext(), shareIntent, null)) {
					startActivity(shareIntent);
				} else {
					mainActivity.showMessage(R.string.feature_not_available_on_this_device, ONStyle.WARN);
				}
				break;
			case "delete":
				removeAttachment(attachmentPosition);
				mAttachmentAdapter.notifyDataSetChanged();
				mGridView.autoresize();
				break;
			case "delete all":
				new MaterialDialog.Builder(mainActivity)
						.title(R.string.delete_all_attachments)
						.positiveText(R.string.confirm)
						.onPositive((materialDialog, dialogAction) -> removeAllAttachments())
						.build()
						.show();
				break;
			case "edit":
				takeSketch(mAttachmentAdapter.getItem(attachmentPosition));
				break;
			default:
				Log.w(Constants.TAG, "No action available");
		}
	}

	private void initViewTitle() {
		title.setText(noteTmp.getTitle());
		title.gatherLinksForText();
		title.setOnTextLinkClickListener(textLinkClickListener);
		// To avoid dropping here the  dragged checklist items
		title.setOnDragListener((v, event) -> {
//					((View)event.getLocalState()).setVisibility(View.VISIBLE);
			return true;
		});
		//When editor action is pressed focus is moved to last character in content field
		title.setOnEditorActionListener((v, actionId, event) -> {
			content.requestFocus();
			content.setSelection(content.getText().length());
			return false;
		});
		requestFocus(title);
	}

	private void initViewContent() {

		content.setText(noteTmp.getContent());
		content.gatherLinksForText();
		content.setOnTextLinkClickListener(textLinkClickListener);
		// Avoids focused line goes under the keyboard
		content.addTextChangedListener(this);

		// Restore checklist
		toggleChecklistView = content;
		if (noteTmp.isChecklist()) {
			noteTmp.setChecklist(false);
			AlphaManager.setAlpha(toggleChecklistView, 0);
			toggleChecklist2();
		}
	}

	/**
	 * Force focus and shows soft keyboard. Only happens if it's a new note, without shared content.
	 * {@link showKeyboard} is used to check if the note is created from shared content.
	 */
	@SuppressWarnings("JavadocReference")
	private void requestFocus(final EditText view) {
		if (note.get_id() == null && !noteTmp.isChanged(note) && showKeyboard) {
			KeyboardUtils.showKeyboard(view);
		}
	}

	/**
	 * Colors tag marker in note's title and content elements
	 */
	private void setTagMarkerColor(Category tag) {

		String colorsPref = prefs.getString("settings_colors_app", Constants.PREF_COLORS_APP_DEFAULT);

		// Checking preference
		if (!"disabled".equals(colorsPref)) {

			// Choosing target view depending on another preference
			ArrayList<View> target = new ArrayList<>();
			if ("complete".equals(colorsPref)) {
				target.add(titleWrapperView);
				target.add(scrollView);
			} else {
				target.add(tagMarkerView);
			}

			// Coloring the target
			if (tag != null && tag.getColor() != null) {
				for (View view : target) {
					view.setBackgroundColor(parseInt(tag.getColor()));
				}
			} else {
				for (View view : target) {
					view.setBackgroundColor(Color.parseColor("#00000000"));
				}
			}
		}
	}

	private void displayLocationDialog() {
		getLocation(new OnGeoUtilResultListenerImpl(mainActivity, mFragment, noteTmp));
	}

	private static class OnGeoUtilResultListenerImpl implements OnGeoUtilResultListener {

		private final WeakReference<MainActivity> mainActivityWeakReference;
		private final WeakReference<DetailFragment> detailFragmentWeakReference;
		private final WeakReference<Note> noteTmpWeakReference;

		OnGeoUtilResultListenerImpl(MainActivity activity, DetailFragment mFragment, Note noteTmp) {
			this.mainActivityWeakReference = new WeakReference<>(activity);
			this.detailFragmentWeakReference = new WeakReference<>(mFragment);
			this.noteTmpWeakReference = new WeakReference<>(noteTmp);
		}

		@Override
		public void onAddressResolved(String address) {
		}

		@Override
		public void onCoordinatesResolved(Location location, String address) {
		}

		@Override
		public void onLocationUnavailable() {
			mainActivityWeakReference.get().showMessage(R.string.location_not_found, ONStyle.ALERT);
		}

		@Override
		public void onLocationRetrieved(Location location) {

			if (!checkWeakReferences()) {
				return;
			}

			if (location == null) {
				return;
			}
			if (!ConnectionManager.internetAvailable(mainActivityWeakReference.get())) {
				noteTmpWeakReference.get().setLatitude(location.getLatitude());
				noteTmpWeakReference.get().setLongitude(location.getLongitude());
				onAddressResolved("");
				return;
			}
			LayoutInflater inflater = mainActivityWeakReference.get().getLayoutInflater();
			View v = inflater.inflate(R.layout.dialog_location, null);
			final AutoCompleteTextView autoCompView = (AutoCompleteTextView) v.findViewById(R.id
					.auto_complete_location);
			autoCompView.setHint(mainActivityWeakReference.get().getString(R.string.search_location));
			autoCompView.setAdapter(new PlacesAutoCompleteAdapter(mainActivityWeakReference.get(), R.layout
					.simple_text_layout));
			final MaterialDialog dialog = new MaterialDialog.Builder(mainActivityWeakReference.get())
					.customView(autoCompView, false)
					.positiveText(R.string.use_current_location)
					.callback(new MaterialDialog.ButtonCallback() {
						@Override
						public void onPositive(MaterialDialog materialDialog) {
							if (TextUtils.isEmpty(autoCompView.getText().toString())) {
								noteTmpWeakReference.get().setLatitude(location.getLatitude());
								noteTmpWeakReference.get().setLongitude(location.getLongitude());
								GeocodeHelper.getAddressFromCoordinates(location, detailFragmentWeakReference.get());
							} else {
								GeocodeHelper.getCoordinatesFromAddress(autoCompView.getText().toString(),
										detailFragmentWeakReference.get());
							}
						}
					})
					.build();
			autoCompView.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					if (s.length() != 0) {
						dialog.setActionButton(DialogAction.POSITIVE, mainActivityWeakReference.get().getString(R
								.string.confirm));
					} else {
						dialog.setActionButton(DialogAction.POSITIVE, mainActivityWeakReference.get().getString(R
								.string
								.use_current_location));
					}
				}

				@Override
				public void afterTextChanged(Editable s) {
				}
			});
			dialog.show();
		}

		private boolean checkWeakReferences() {
			return mainActivityWeakReference.get() != null && !mainActivityWeakReference.get().isFinishing()
					&& detailFragmentWeakReference.get() != null && noteTmpWeakReference.get() != null;
		}
	}

	@Override
	public void onLocationRetrieved(Location location) {
		if (location == null) {
			mainActivity.showMessage(R.string.location_not_found, ONStyle.ALERT);
		}
		if (location != null) {
			noteTmp.setLatitude(location.getLatitude());
			noteTmp.setLongitude(location.getLongitude());
			if (!TextUtils.isEmpty(noteTmp.getAddress())) {
				locationTextView.setVisibility(View.VISIBLE);
				locationTextView.setText(noteTmp.getAddress());
			} else {
				GeocodeHelper.getAddressFromCoordinates(location, mFragment);
			}
		}
	}

	@Override
	public void onLocationUnavailable() {
		mainActivity.showMessage(R.string.location_not_found, ONStyle.ALERT);
	}

	@Override
	public void onAddressResolved(String address) {
		if (TextUtils.isEmpty(address)) {
			if (!isNoteLocationValid()) {
				mainActivity.showMessage(R.string.location_not_found, ONStyle.ALERT);
				return;
			}
			address = noteTmp.getLatitude() + ", " + noteTmp.getLongitude();
		}
		if (!GeocodeHelper.areCoordinates(address)) {
			noteTmp.setAddress(address);
		}
		locationTextView.setVisibility(View.VISIBLE);
		locationTextView.setText(address);
		fade(locationTextView, true);
	}

	@Override
	public void onCoordinatesResolved(Location location, String address) {
		if (location != null) {
			noteTmp.setLatitude(location.getLatitude());
			noteTmp.setLongitude(location.getLongitude());
			noteTmp.setAddress(address);
			locationTextView.setVisibility(View.VISIBLE);
			locationTextView.setText(address);
			fade(locationTextView, true);
		} else {
			mainActivity.showMessage(R.string.location_not_found, ONStyle.ALERT);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_detail, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {

		// Closes search view if left open in List fragment
		MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
		if (searchMenuItem != null) {
			MenuItemCompat.collapseActionView(searchMenuItem);
		}

		boolean newNote = noteTmp.get_id() == null;

		// If note is trashed only this options will be available from menu
		if (noteTmp.isTrashed()) {
			menu.findItem(R.id.menu_untrash).setVisible(true);
			menu.findItem(R.id.menu_delete).setVisible(true);
			// Otherwise all other actions will be available
		} else {
			menu.findItem(R.id.menu_trash).setVisible(!newNote);
			menu.findItem(R.id.menu_attachment).setVisible(newNote);
			menu.findItem(R.id.menu_tag).setVisible(newNote);
		}
	}

	@SuppressLint("NewApi")
	public boolean goHome() {
		stopPlaying();

		// The activity has managed a shared intent from third party app and
		// performs a normal onBackPressed instead of returning back to ListActivity
		if (!afterSavedReturnsToList) {
			if (!TextUtils.isEmpty(exitMessage)) {
				mainActivity.showToast(exitMessage, Toast.LENGTH_SHORT);
			}
			mainActivity.finish();

		} else {

			if (!TextUtils.isEmpty(exitMessage) && exitCroutonStyle != null) {
				mainActivity.showMessage(exitMessage, exitCroutonStyle);
			}

			// Otherwise the result is passed to ListActivity
			if (mainActivity != null && mainActivity.getSupportFragmentManager() != null) {
				mainActivity.getSupportFragmentManager().popBackStack();
				if (mainActivity.getSupportFragmentManager().getBackStackEntryCount() == 1) {
					mainActivity.getSupportActionBar().setDisplayShowTitleEnabled(true);
					if (mainActivity.getDrawerToggle() != null) {
						mainActivity.getDrawerToggle().setDrawerIndicatorEnabled(true);
					}
					EventBus.getDefault().post(new SwitchFragmentEvent(SwitchFragmentEvent.Direction.PARENT));
				}
			}
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_attachment:
				showAttachmentsPopup();
				break;
			case R.id.menu_tag:
				addTags();
				break;
			case R.id.menu_category:
				categorizeNote();
				break;
			case R.id.menu_trash:
				trashNote(true);
				break;
			case R.id.menu_untrash:
				trashNote(false);
				break;
			case R.id.menu_delete:
				deleteNote();
				break;
			case R.id.menu_note_info:
				printLabel();
				//showNoteInfo();
				break;
			case R.id.upload_evidence:
				upload();
				break;
			default:
				Log.w(Constants.TAG, "Invalid menu option selected");
		}

		((OmniNotes) getActivity().getApplication()).getAnalyticsHelper().trackActionFromResourceId(getActivity(),
				item.getItemId());

		return super.onOptionsItemSelected(item);
	}

	private void initViewAttachments() {

		// Attachments position based on preferences
		if (prefs.getBoolean(Constants.PREF_ATTANCHEMENTS_ON_BOTTOM, false)) {
			attachmentsBelow.inflate();
		} else {
			attachmentsAbove.inflate();
		}
		mGridView = (ExpandableHeightGridView) root.findViewById(R.id.gridview);

		// Some fields can be filled by third party application and are always shown
		mAttachmentAdapter = new AttachmentAdapter(mainActivity, noteTmp.getAttachmentsList(), mGridView);

		// Initialzation of gridview for images
		mGridView.setAdapter(mAttachmentAdapter);
		mGridView.autoresize();

		// Click events for images in gridview (zooms image)
		mGridView.setOnItemClickListener((parent, v, position, id) -> {
			Attachment attachment = (Attachment) parent.getAdapter().getItem(position);
			Uri uri = attachment.getUri();
			Intent attachmentIntent;
			if (Constants.MIME_TYPE_FILES.equals(attachment.getMime_type())) {

				attachmentIntent = new Intent(Intent.ACTION_VIEW);
				attachmentIntent.setDataAndType(uri, StorageHelper.getMimeType(mainActivity,
						attachment.getUri()));
				attachmentIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent
						.FLAG_GRANT_WRITE_URI_PERMISSION);
				if (IntentChecker.isAvailable(mainActivity.getApplicationContext(), attachmentIntent, null)) {
					startActivity(attachmentIntent);
				} else {
					mainActivity.showMessage(R.string.feature_not_available_on_this_device, ONStyle.WARN);
				}

				// Media files will be opened in internal gallery
			} else if (Constants.MIME_TYPE_IMAGE.equals(attachment.getMime_type())
					|| Constants.MIME_TYPE_SKETCH.equals(attachment.getMime_type())
					|| Constants.MIME_TYPE_VIDEO.equals(attachment.getMime_type())) {
				// Title
				noteTmp.setTitle(getNoteTitle());
				noteTmp.setContent(getNoteContent());
				String title1 = TextHelper.parseTitleAndContent(mainActivity,
						noteTmp)[0].toString();
				// Images
				int clickedImage = 0;
				ArrayList<Attachment> images = new ArrayList<>();
				for (Attachment mAttachment : noteTmp.getAttachmentsList()) {
					if (Constants.MIME_TYPE_IMAGE.equals(mAttachment.getMime_type())
							|| Constants.MIME_TYPE_SKETCH.equals(mAttachment.getMime_type())
							|| Constants.MIME_TYPE_VIDEO.equals(mAttachment.getMime_type())) {
						images.add(mAttachment);
						if (mAttachment.equals(attachment)) {
							clickedImage = images.size() - 1;
						}
					}
				}
				// Intent
				attachmentIntent = new Intent(mainActivity, GalleryActivity.class);
				attachmentIntent.putExtra(Constants.GALLERY_TITLE, title1);
				attachmentIntent.putParcelableArrayListExtra(Constants.GALLERY_IMAGES, images);
				attachmentIntent.putExtra(Constants.GALLERY_CLICKED_IMAGE, clickedImage);
				startActivity(attachmentIntent);

			} else if (Constants.MIME_TYPE_AUDIO.equals(attachment.getMime_type())) {
				playback(v, attachment.getUri());
			}

		});

		mGridView.setOnItemLongClickListener((parent, v, position, id) -> {
			// To avoid deleting audio attachment during playback
			if (mPlayer != null) return false;
			List<String> items = Arrays.asList(getResources().getStringArray(R.array.attachments_actions));
			if (!Constants.MIME_TYPE_SKETCH.equals(mAttachmentAdapter.getItem(position).getMime_type())) {
				items = items.subList(0, items.size() - 1);
			}
			Attachment attachment = mAttachmentAdapter.getItem(position);
			new MaterialDialog.Builder(mainActivity)
					.title(attachment.getName() + " (" + AttachmentsHelper.getSize(attachment) + ")")
					.items(items.toArray(new String[items.size()]))
					.itemsCallback((materialDialog, view, i, charSequence) ->
							performAttachmentAction(position, i))
					.build()
					.show();
			return true;
		});
	}

	private void upload() {
		//evi.picture;
		//evi.signiture;
		//evi.record;
		MultipartBody.Part picture = null;
		MultipartBody.Part signiture = null;

		for (Attachment attachment : noteTmp.getAttachmentsList()) {
			if (Constants.MIME_TYPE_IMAGE.equals(attachment.getMime_type())) {
				//attachment.getUriPath();
				//attachment.getUri();
				String fname = attachment.getUri().toString().split("///")[1];
				File f = new File(fname);
				RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), f);
				picture = MultipartBody.Part.createFormData("picture", f.getName(), reqFile);
			} else if (Constants.MIME_TYPE_SKETCH.equals(attachment.getMime_type())) {
				String fname = attachment.getUri().toString().split("///")[1];
				File f = new File(fname);
				RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), f);
				signiture = MultipartBody.Part.createFormData("signiture", f.getName(), reqFile);
			} else if (Constants.MIME_TYPE_AUDIO.equals(attachment.getMime_type())) {
				//evi.record = new File(attachment.getUri().toString());
			}
		}

		Call<ResponseBody> call = cemsApi.post_evidence(
				RequestBody.create(MediaType.parse("text/plain"), String.valueOf(noteTmp.getCategory().getId())),
				RequestBody.create(MediaType.parse("text/plain"), getEviType()),
				RequestBody.create(MediaType.parse("text/plain"), getNoteTitle() + "\n" + getNoteContent()),
				RequestBody.create(MediaType.parse("text/plain"), getEviTime()),
				picture,
				signiture);
		call.enqueue(new Callback<ResponseBody>() {
			@Override
			public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
				if( response.isSuccessful()) {}
				else {}
			}

			@Override
			public void onFailure(Call<ResponseBody> call, Throwable t) {

			}
		});
	}

	private String getEviTime() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return dateFormat.format(new Date().getTime());
	}

	private String getEviType() {
		String content = getNoteContent();
		String[] tags = content.split("#");
		if (tags.length == 1) return "";
		else {
			String[] frontTag1 = tags[1].split("\n");
			String[] frontTag2 = frontTag1[0].split(" ");
			return frontTag2[0];
		}
	}

	private void showNoteInfo() {
		noteTmp.setTitle(getNoteTitle());
		noteTmp.setContent(getNoteContent());
		Intent intent = new Intent(getContext(), NoteInfosActivity.class);
		intent.putExtra(Constants.INTENT_NOTE, (android.os.Parcelable) noteTmp);
		startActivity(intent);

	}

	private void printLabel() {
		// 라벨 출력!
		if (!initializeObject()) {
			return;
		}

		if (!createData()) {
			finalizeObject();
			return;
		}

		if (!printData()) {
			finalizeObject();
			return;
		}
	}

	private boolean initializeObject() {
		try {
			mPrinter = new Printer(Printer.TM_P20, Printer.MODEL_ANK, getActivity().getApplicationContext());
		}
		catch (Exception e) {
			ShowMsg.showException(e, "Printer", getActivity().getApplicationContext());
			return false;
		}

		mPrinter.setReceiveEventListener(this);

		return true;
	}

	@Override
	public void onPtrReceive(final Printer printerObj, final int code, final PrinterStatusInfo status, final String printJobId) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public synchronized void run() {
				ShowMsg.showResult(code, makeErrorMessage(status), getActivity());

				dispPrinterWarnings(status);

				new Thread(new Runnable() {
					@Override
					public void run() {
						disconnectPrinter();
					}
				}).start();
			}
		});
	}

	private void finalizeObject() {
		if (mPrinter == null) {
			return;
		}

		mPrinter.clearCommandBuffer();

		mPrinter.setReceiveEventListener(null);

		mPrinter = null;
	}

	private boolean printData() {
		if (mPrinter == null) {
			return false;
		}

		if (!connectPrinter()) {
			return false;
		}

		PrinterStatusInfo status = mPrinter.getStatus();

		dispPrinterWarnings(status);

		if (!isPrintable(status)) {
			ShowMsg.showMsg(makeErrorMessage(status), getActivity().getApplicationContext());
			try {
				mPrinter.disconnect();
			}
			catch (Exception ex) {
				// Do nothing
			}
			return false;
		}

		try {
			mPrinter.sendData(Printer.PARAM_DEFAULT);
		}
		catch (Exception e) {
			ShowMsg.showException(e, "sendData", getActivity().getApplicationContext());
			try {
				mPrinter.disconnect();
			}
			catch (Exception ex) {
				// Do nothing
			}
			return false;
		}

		return true;
	}
	private boolean connectPrinter() {
		boolean isBeginTransaction = false;

		if (mPrinter == null) {
			return false;
		}

		try {
			mPrinter.connect("BT:00:01:90:AE:B9:D8", Printer.PARAM_DEFAULT);
		}
		catch (Exception e) {
			ShowMsg.showException(e, "connect", getActivity().getApplicationContext());
			return false;
		}

		try {
			mPrinter.beginTransaction();
			isBeginTransaction = true;
		}
		catch (Exception e) {
			ShowMsg.showException(e, "beginTransaction", getActivity().getApplicationContext());
		}

		if (isBeginTransaction == false) {
			try {
				mPrinter.disconnect();
			}
			catch (Epos2Exception e) {
				// Do nothing
				return false;
			}
		}

		return true;
	}

	private void dispPrinterWarnings(PrinterStatusInfo status) {
		if (status == null) {
			return;
		}

		if (status.getPaper() == Printer.PAPER_NEAR_END) {
			//warningsMsg += getString(R.string.handlingmsg_warn_receipt_near_end);
			Toast.makeText(getActivity().getApplicationContext(), "Roll paper is nearly end.", Toast.LENGTH_LONG).show();
		}

		if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_1) {
			//warningsMsg += getString(R.string.handlingmsg_warn_battery_near_end);
			Toast.makeText(getActivity().getApplicationContext(), "Battery level of printer is low.", Toast.LENGTH_LONG).show();
		}
	}

	private boolean isPrintable(PrinterStatusInfo status) {
		if (status == null) {
			return false;
		}

		if (status.getConnection() == Printer.FALSE) {
			return false;
		}
		else if (status.getOnline() == Printer.FALSE) {
			return false;
		}
		else {
			;//print available
		}

		return true;
	}

	private String makeErrorMessage(PrinterStatusInfo status) {
		String msg = "";

		if (status.getOnline() == Printer.FALSE) {
			msg += getString(R.string.handlingmsg_err_offline);
		}
		if (status.getConnection() == Printer.FALSE) {
			msg += getString(R.string.handlingmsg_err_no_response);
		}
		if (status.getCoverOpen() == Printer.TRUE) {
			msg += getString(R.string.handlingmsg_err_cover_open);
		}
		if (status.getPaper() == Printer.PAPER_EMPTY) {
			msg += getString(R.string.handlingmsg_err_receipt_end);
		}
		if (status.getPaperFeed() == Printer.TRUE || status.getPanelSwitch() == Printer.SWITCH_ON) {
			msg += getString(R.string.handlingmsg_err_paper_feed);
		}
		if (status.getErrorStatus() == Printer.MECHANICAL_ERR || status.getErrorStatus() == Printer.AUTOCUTTER_ERR) {
			msg += getString(R.string.handlingmsg_err_autocutter);
			msg += getString(R.string.handlingmsg_err_need_recover);
		}
		if (status.getErrorStatus() == Printer.UNRECOVER_ERR) {
			msg += getString(R.string.handlingmsg_err_unrecover);
		}
		if (status.getErrorStatus() == Printer.AUTORECOVER_ERR) {
			if (status.getAutoRecoverError() == Printer.HEAD_OVERHEAT) {
				msg += getString(R.string.handlingmsg_err_overheat);
				msg += getString(R.string.handlingmsg_err_head);
			}
			if (status.getAutoRecoverError() == Printer.MOTOR_OVERHEAT) {
				msg += getString(R.string.handlingmsg_err_overheat);
				msg += getString(R.string.handlingmsg_err_motor);
			}
			if (status.getAutoRecoverError() == Printer.BATTERY_OVERHEAT) {
				msg += getString(R.string.handlingmsg_err_overheat);
				msg += getString(R.string.handlingmsg_err_battery);
			}
			if (status.getAutoRecoverError() == Printer.WRONG_PAPER) {
				msg += getString(R.string.handlingmsg_err_wrong_paper);
			}
		}
		if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_0) {
			msg += getString(R.string.handlingmsg_err_battery_real_end);
		}

		return msg;
	}

	private void disconnectPrinter() {
		if (mPrinter == null) {
			return;
		}

		try {
			mPrinter.endTransaction();
		}
		catch (final Exception e) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public synchronized void run() {
					ShowMsg.showException(e, "endTransaction", getActivity());
				}
			});
		}

		try {
			mPrinter.disconnect();
		}
		catch (final Exception e) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public synchronized void run() {
					ShowMsg.showException(e, "disconnect", getActivity());
				}
			});
		}

		finalizeObject();
	}

	private boolean createData() {
		String method = "";
		Bitmap logoData = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
		StringBuilder textData = new StringBuilder();

		if (mPrinter == null) {
			return false;
		}

		try {
			mPrinter.addTextAlign(Printer.ALIGN_CENTER);
			mPrinter.addImage(logoData, 0, 0,
					logoData.getWidth(),
					logoData.getHeight(),
					Printer.COLOR_1,
					Printer.MODE_MONO,
					Printer.HALFTONE_DITHER,
					Printer.PARAM_DEFAULT,
					Printer.COMPRESS_AUTO);
			mPrinter.addFeedLine(1);
			mPrinter.addText("* Criminal Evidence Management System *\n");
			mPrinter.addFeedLine(1);
			mPrinter.addText("------------------------------------------\n");
			mPrinter.addFeedLine(1);

			mPrinter.addTextSize(2, 2);
			String title = "Case. " + noteTmp.getCategory().toString() + "\n";
			mPrinter.addText(title);
			mPrinter.addTextSize(1, 1);
			mPrinter.addFeedLine(1);
			mPrinter.addTextSize(2, 2);
			title = "Evi. " + ((EditText) titleWrapperView.findViewById(R.id.detail_title)).getText().toString() + "\n";
			mPrinter.addText(title);
			mPrinter.addTextSize(1, 1);
			mPrinter.addFeedLine(1);
			mPrinter.addText("------------------------------------------\n");
			mPrinter.addFeedLine(1);
			mPrinter.addTextAlign(Printer.ALIGN_LEFT);
			mPrinter.addText(content.getText().toString()+"\n");
			mPrinter.addFeedLine(1);
			mPrinter.addTextAlign(Printer.ALIGN_CENTER);
			mPrinter.addText("------------------------------------------\n");
			mPrinter.addFeedLine(1);
			View capture = mGridView;
			capture.setDrawingCacheEnabled(true);
			capture.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
			capture.buildDrawingCache();
			if(capture.getDrawingCache() != null) {
				Bitmap snapshot = Bitmap.createBitmap(capture.getDrawingCache());
				snapshot = Bitmap.createScaledBitmap(snapshot, 384, snapshot.getHeight() * 384 / snapshot.getWidth(), true);
				capture.setDrawingCacheEnabled(false);
				capture.destroyDrawingCache();

				mPrinter.addImage(snapshot, 0, 0,
						snapshot.getWidth(),
						snapshot.getHeight(),
						Printer.COLOR_1,
						Printer.MODE_MONO_HIGH_DENSITY,
						Printer.PARAM_DEFAULT,
						Printer.PARAM_DEFAULT,
						Printer.PARAM_DEFAULT);
			}

			method = "addFeedLine";
			mPrinter.addFeedLine(1);
			mPrinter.addText("------------------------------------------\n");
			// Whatever you need to encode in the QR code
			MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
			try {
				// TODO: QR code = URL Link
				BitMatrix bitMatrix = multiFormatWriter.encode(title, BarcodeFormat.QR_CODE,200,200);
				BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
				Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
				mPrinter.addImage(bitmap, 0, 0,
						bitmap.getWidth(),
						bitmap.getHeight(),
						Printer.COLOR_1,
						Printer.MODE_MONO,
						Printer.HALFTONE_DITHER,
						Printer.PARAM_DEFAULT,
						Printer.COMPRESS_AUTO);
			} catch (WriterException e) {
				e.printStackTrace();
			}

			/*
			method = "addBarcode";
			mPrinter.addBarcode("01209457",
					Printer.BARCODE_CODE39,
					Printer.HRI_BELOW,
					Printer.FONT_A,
					2,
					100);
			*/

			mPrinter.addCut(Printer.CUT_FEED);
		}
		catch (Exception e) {
			ShowMsg.showException(e, method, getActivity());
			return false;
		}

		return true;
	}

	private void navigateUp() {
		afterSavedReturnsToList = true;
		saveAndExit(this);
	}

	/**
	 * Toggles checklist view
	 */
	private void toggleChecklist2() {
		boolean keepChecked = prefs.getBoolean(Constants.PREF_KEEP_CHECKED, true);
		boolean showChecks = prefs.getBoolean(Constants.PREF_KEEP_CHECKMARKS, true);
		toggleChecklist2(keepChecked, showChecks);
	}

	@SuppressLint("NewApi")
	private void toggleChecklist2(final boolean keepChecked, final boolean showChecks) {
		// Get instance and set options to convert EditText to CheckListView

		mChecklistManager = mChecklistManager == null ? new ChecklistManager(mainActivity) : mChecklistManager;
		int checkedItemsBehavior = Integer.valueOf(prefs.getString("settings_checked_items_behavior", String.valueOf
				(it.feio.android.checklistview.Settings.CHECKED_HOLD)));
		mChecklistManager
				.showCheckMarks(showChecks)
				.newEntryHint(getString(R.string.checklist_item_hint))
				.keepChecked(keepChecked)
				.undoBarContainerView(scrollView)
				.moveCheckedOnBottom(checkedItemsBehavior);

		// Links parsing options
		mChecklistManager.setOnTextLinkClickListener(textLinkClickListener);
		mChecklistManager.addTextChangedListener(mFragment);
		mChecklistManager.setCheckListChangedListener(mFragment);

		// Switches the views
		View newView = null;
		try {
			newView = mChecklistManager.convert(toggleChecklistView);
		} catch (ViewNotSupportedException e) {
			Log.e(Constants.TAG, "Error switching checklist view", e);
		}

		// Switches the views
		if (newView != null) {
			mChecklistManager.replaceViews(toggleChecklistView, newView);
			toggleChecklistView = newView;
			animate(toggleChecklistView).alpha(1).scaleXBy(0).scaleX(1).scaleYBy(0).scaleY(1);
			noteTmp.setChecklist(!noteTmp.isChecklist());
		}
	}

	/**
	 * Categorize note choosing from a list of previously created categories
	 */
	private void categorizeNote() {
		// Retrieves all available categories
		final ArrayList<Category> categories = DbHelper.getInstance().getCategories();

		String currentCategory = noteTmp.getCategory() != null ? String.valueOf(noteTmp.getCategory().getId()) : null;

		final MaterialDialog dialog = new MaterialDialog.Builder(mainActivity)
				.title(R.string.categorize_as)
				.adapter(new NavDrawerCategoryAdapter(mainActivity, categories, currentCategory), null)
				.positiveText(R.string.add_category)
				.positiveColorRes(R.color.colorPrimary)
				.negativeText(R.string.remove_category)
				.negativeColorRes(R.color.colorAccent)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog dialog) {
						Intent intent = new Intent(mainActivity, CategoryActivity.class);
						intent.putExtra("noHome", true);
						startActivityForResult(intent, CATEGORY);
					}

					@Override
					public void onNegative(MaterialDialog dialog) {
						noteTmp.setCategory(null);
						setTagMarkerColor(null);
					}
				})
				.build();

		dialog.getListView().setOnItemClickListener((parent, view, position, id) -> {
			noteTmp.setCategory(categories.get(position));
			setTagMarkerColor(categories.get(position));
			dialog.dismiss();
		});

		dialog.show();
	}

	private void showAttachmentsPopup() {
		LayoutInflater inflater = mainActivity.getLayoutInflater();
		final View layout = inflater.inflate(R.layout.attachment_dialog, null);

		attachmentDialog = new MaterialDialog.Builder(mainActivity)
				.autoDismiss(false)
				.customView(layout, false)
				.build();
		attachmentDialog.show();

		// Camera
		android.widget.TextView cameraSelection = (android.widget.TextView) layout.findViewById(R.id.camera);
		cameraSelection.setOnClickListener(new AttachmentOnClickListener());
		// Audio recording
		android.widget.TextView recordingSelection = (android.widget.TextView) layout.findViewById(R.id.recording);
		toggleAudioRecordingStop(recordingSelection);
		recordingSelection.setOnClickListener(new AttachmentOnClickListener());
		// Video recording
		android.widget.TextView videoSelection = (android.widget.TextView) layout.findViewById(R.id.video);
		videoSelection.setOnClickListener(new AttachmentOnClickListener());
		// Sketch
		android.widget.TextView sketchSelection = (android.widget.TextView) layout.findViewById(R.id.sketch);
		sketchSelection.setOnClickListener(new AttachmentOnClickListener());
		// Location
		android.widget.TextView locationSelection = (android.widget.TextView) layout.findViewById(R.id.location);
		locationSelection.setOnClickListener(new AttachmentOnClickListener());
		// Time
		android.widget.TextView timeStampSelection = (android.widget.TextView) layout.findViewById(R.id.timestamp);
		timeStampSelection.setOnClickListener(new AttachmentOnClickListener());
	}

	private void takePhoto() {
		// Checks for camera app available
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (!IntentChecker.isAvailable(mainActivity, intent, new String[]{PackageManager.FEATURE_CAMERA})) {
			mainActivity.showMessage(R.string.feature_not_available_on_this_device, ONStyle.ALERT);

			return;
		}
		// Checks for created file validity
		File f = StorageHelper.createNewAttachmentFile(mainActivity, Constants.MIME_TYPE_IMAGE_EXT);
		if (f == null) {
			mainActivity.showMessage(R.string.error, ONStyle.ALERT);
			return;
		}
		// Launches intent
		attachmentUri = Uri.fromFile(f);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);
		startActivityForResult(intent, TAKE_PHOTO);
	}

	private void takeVideo() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		if (!IntentChecker.isAvailable(mainActivity, takeVideoIntent, new String[]{PackageManager.FEATURE_CAMERA})) {
			mainActivity.showMessage(R.string.feature_not_available_on_this_device, ONStyle.ALERT);

			return;
		}
		// File is stored in custom ON folder to speedup the attachment
		File f = StorageHelper.createNewAttachmentFile(mainActivity, Constants.MIME_TYPE_VIDEO_EXT);
		if (f == null) {
			mainActivity.showMessage(R.string.error, ONStyle.ALERT);
			return;
		}
		attachmentUri = Uri.fromFile(f);
		takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, attachmentUri);
		String maxVideoSizeStr = "".equals(prefs.getString("settings_max_video_size",
				"")) ? "0" : prefs.getString("settings_max_video_size", "");
		long maxVideoSize = parseLong(maxVideoSizeStr) * 1024L * 1024L;
		takeVideoIntent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, maxVideoSize);
		startActivityForResult(takeVideoIntent, TAKE_VIDEO);
	}

	private void takeSketch(Attachment attachment) {

		File f = StorageHelper.createNewAttachmentFile(mainActivity, Constants.MIME_TYPE_SKETCH_EXT);
		if (f == null) {
			mainActivity.showMessage(R.string.error, ONStyle.ALERT);
			return;
		}
		attachmentUri = Uri.fromFile(f);

		// Forces portrait orientation to this fragment only
		mainActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Fragments replacing
		FragmentTransaction transaction = mainActivity.getSupportFragmentManager().beginTransaction();
		mainActivity.animateTransition(transaction, mainActivity.TRANSITION_HORIZONTAL);
		SketchFragment mSketchFragment = new SketchFragment();
		Bundle b = new Bundle();
		b.putParcelable(MediaStore.EXTRA_OUTPUT, attachmentUri);
		if (attachment != null) {
			b.putParcelable("base", attachment.getUri());
		}
		mSketchFragment.setArguments(b);
		transaction.replace(R.id.fragment_container, mSketchFragment, mainActivity.FRAGMENT_SKETCH_TAG)
				.addToBackStack(mainActivity.FRAGMENT_DETAIL_TAG).commit();
	}

	private void addTimestamp() {
		Editable editable = content.getText();
		int position = content.getSelectionStart();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-d'T'HH:mm:ssZ");


		String dateStamp = dateFormat.format(new Date().getTime()) + " ";
		if (noteTmp.isChecklist()) {
			if (mChecklistManager.getFocusedItemView() != null) {
				editable = mChecklistManager.getFocusedItemView().getEditText().getEditableText();
				position = mChecklistManager.getFocusedItemView().getEditText().getSelectionStart();
			} else {
				((CheckListView) toggleChecklistView)
						.addItem(dateStamp, false, mChecklistManager.getCount());
			}
		}
		String leadSpace = position == 0 ? "" : " ";
		dateStamp = leadSpace + dateStamp;
		editable.insert(position, dateStamp);
		Selection.setSelection(editable, position + dateStamp.length());
	}

	@SuppressLint("NewApi")
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		// Fetch uri from activities, store into adapter and refresh adapter
		Attachment attachment;
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
				case TAKE_PHOTO:
					attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_IMAGE);
					addAttachment(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();
					break;
				case TAKE_VIDEO:
					// Gingerbread doesn't allow custom folder so data are retrieved from intent
					if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
						attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_VIDEO);
					} else {
						attachment = new Attachment(intent.getData(), Constants.MIME_TYPE_VIDEO);
					}
					addAttachment(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();
					break;
				case FILES:
					onActivityResultManageReceivedFiles(intent);
					break;
				case SET_PASSWORD:
					noteTmp.setPasswordChecked(true);
					lockUnlock();
					break;
				case SKETCH:
					attachment = new Attachment(attachmentUri, Constants.MIME_TYPE_SKETCH);
					addAttachment(attachment);
					mAttachmentAdapter.notifyDataSetChanged();
					mGridView.autoresize();
					break;
				case CATEGORY:
					mainActivity.showMessage(R.string.category_saved, ONStyle.CONFIRM);
					Category category = intent.getParcelableExtra("category");
					noteTmp.setCategory(category);
					setTagMarkerColor(category);
					break;
				case DETAIL:
					mainActivity.showMessage(R.string.note_updated, ONStyle.CONFIRM);
					break;
				default:
					Log.e(Constants.TAG, "Wrong element choosen: " + requestCode);
			}
		}
	}

	private void onActivityResultManageReceivedFiles(Intent intent) {
		List<Uri> uris = new ArrayList<>();
		if (Build.VERSION.SDK_INT > 16 && intent.getClipData() != null) {
			for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
				uris.add(intent.getClipData().getItemAt(i).getUri());
			}
		} else {
			uris.add(intent.getData());
		}
		for (Uri uri : uris) {
			String name = FileHelper.getNameFromUri(mainActivity, uri);
			new AttachmentTask(this, uri, name, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	@SuppressLint("NewApi")
	private void trashNote(boolean trash) {
		// Simply go back if is a new note
		if (noteTmp.get_id() == null) {
			goHome();
			return;
		}

		noteTmp.setTrashed(trash);
		goBack = true;
		exitMessage = trash ? getString(R.string.note_trashed) : getString(R.string.note_untrashed);
		exitCroutonStyle = trash ? ONStyle.WARN : ONStyle.INFO;
		if (trash) {
			ShortcutHelper.removeshortCut(OmniNotes.getAppContext(), noteTmp);
			ReminderHelper.removeReminder(OmniNotes.getAppContext(), noteTmp);
		} else {
			ReminderHelper.addReminder(OmniNotes.getAppContext(), note);
		}
		saveNote(this);
	}

	private void deleteNote() {
		new MaterialDialog.Builder(mainActivity)
				.content(R.string.delete_note_confirmation)
				.positiveText(R.string.ok)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						mainActivity.deleteNote(noteTmp);
						Log.d(Constants.TAG, "Deleted note with id '" + noteTmp.get_id() + "'");
						mainActivity.showMessage(R.string.note_deleted, ONStyle.ALERT);
						goHome();
					}
				}).build().show();
	}

	public void saveAndExit(OnNoteSaved mOnNoteSaved) {
		if (isAdded()) {
			exitMessage = getString(R.string.note_updated);
			exitCroutonStyle = ONStyle.CONFIRM;
			goBack = true;
			saveNote(mOnNoteSaved);
		}
	}

	/**
	 * Save new notes, modify them or archive
	 */
	void saveNote(OnNoteSaved mOnNoteSaved) {

		// Changed fields
		noteTmp.setTitle(getNoteTitle());
		noteTmp.setContent(getNoteContent());

		// Check if some text or attachments of any type have been inserted or is an empty note
		if (goBack && TextUtils.isEmpty(noteTmp.getTitle()) && TextUtils.isEmpty(noteTmp.getContent())
				&& noteTmp.getAttachmentsList().size() == 0) {
			Log.d(Constants.TAG, "Empty note not saved");
			exitMessage = getString(R.string.empty_note_not_saved);
			exitCroutonStyle = ONStyle.INFO;
			goHome();
			return;
		}

		if (saveNotNeeded()) {
			exitMessage = "";
			if (goBack) {
				goHome();
			}
			return;
		}

		noteTmp.setAttachmentsListOld(note.getAttachmentsList());

		new SaveNoteTask(mOnNoteSaved, lastModificationUpdatedNeeded()).executeOnExecutor(AsyncTask
				.THREAD_POOL_EXECUTOR, noteTmp);
	}

	/**
	 * Checks if nothing is changed to avoid committing if possible (check)
	 */
	private boolean saveNotNeeded() {
		if (noteTmp.get_id() == null && prefs.getBoolean(Constants.PREF_AUTO_LOCATION, false)) {
			note.setLatitude(noteTmp.getLatitude());
			note.setLongitude(noteTmp.getLongitude());
		}
		return !noteTmp.isChanged(note) || (noteTmp.isLocked() && !noteTmp.isPasswordChecked());
	}

	/**
	 * Checks if only tag, archive or trash status have been changed
	 * and then force to not update last modification date*
	 */
	private boolean lastModificationUpdatedNeeded() {
		note.setCategory(noteTmp.getCategory());
		note.setArchived(noteTmp.isArchived());
		note.setTrashed(noteTmp.isTrashed());
		note.setLocked(noteTmp.isLocked());
		return noteTmp.isChanged(note);
	}

	@Override
	public void onNoteSaved(Note noteSaved) {
		MainActivity.notifyAppWidgets(OmniNotes.getAppContext());
		if (!activityPausing) {
			EventBus.getDefault().post(new NotesUpdatedEvent());
			deleteMergedNotes(mergedNotesIds);
			if (noteTmp.getAlarm() != null && !noteTmp.getAlarm().equals(note.getAlarm())) {
				ReminderHelper.showReminderMessage(noteTmp.getAlarm());
			}
		}
		note = new Note(noteSaved);
		if (goBack) {
			goHome();
		}
	}

	private void deleteMergedNotes(List<String> mergedNotesIds) {
		ArrayList<Note> notesToDelete = new ArrayList<Note>();
		if (mergedNotesIds != null) {
			for (String mergedNoteId : mergedNotesIds) {
				Note note = new Note();
				note.set_id(Long.valueOf(mergedNoteId));
				notesToDelete.add(note);
			}
			new NoteProcessorDelete(notesToDelete).process();
		}
	}

	private String getNoteTitle() {
		if (title != null && !TextUtils.isEmpty(title.getText())) {
			return title.getText().toString();
		} else {
			return "";
		}
	}

	private String getNoteContent() {
		String contentText = "";
		if (!noteTmp.isChecklist()) {
			// Due to checklist library introduction the returned EditText class is no more a
			// com.neopixl.pixlui.components.edittext.EditText but a standard android.widget.EditText
			View contentView = root.findViewById(R.id.detail_content);
			if (contentView instanceof EditText) {
				contentText = ((EditText) contentView).getText().toString();
			} else if (contentView instanceof android.widget.EditText) {
				contentText = ((android.widget.EditText) contentView).getText().toString();
			}
		} else {
			if (mChecklistManager != null) {
				mChecklistManager.keepChecked(true).showCheckMarks(true);
				contentText = mChecklistManager.getText();
			}
		}
		return contentText;
	}

	private void lockUnlock() {
		// Empty password has been set
		if (prefs.getString(Constants.PREF_PASSWORD, null) == null) {
			mainActivity.showMessage(R.string.password_not_set, ONStyle.WARN);
			return;
		}
		mainActivity.showMessage(R.string.save_note_to_lock_it, ONStyle.INFO);
		mainActivity.supportInvalidateOptionsMenu();
		noteTmp.setLocked(!noteTmp.isLocked());
		noteTmp.setPasswordChecked(true);
	}

	/**
	 * Audio recordings playback
	 */
	private void playback(View v, Uri uri) {
		// Some recording is playing right now
		if (mPlayer != null && mPlayer.isPlaying()) {
			if (isPlayingView != v) {
				// If the audio actually played is NOT the one from the click view the last one is played
				stopPlaying();
				isPlayingView = v;
				startPlaying(uri);
				replacePlayingAudioBitmap(v);
			} else {
				// Otherwise just stops playing
				stopPlaying();
			}
		} else {
			// If nothing is playing audio just plays
			isPlayingView = v;
			startPlaying(uri);
			replacePlayingAudioBitmap(v);
		}
	}

	private void replacePlayingAudioBitmap(View v) {
		Drawable d = ((ImageView) v.findViewById(R.id.gridview_item_picture)).getDrawable();
		if (BitmapDrawable.class.isAssignableFrom(d.getClass())) {
			recordingBitmap = ((BitmapDrawable) d).getBitmap();
		} else {
			recordingBitmap = ((GlideBitmapDrawable) d.getCurrent()).getBitmap();
		}
		((ImageView) v.findViewById(R.id.gridview_item_picture)).setImageBitmap(ThumbnailUtils
				.extractThumbnail(BitmapFactory.decodeResource(mainActivity.getResources(),
						R.drawable.stop), Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE));
	}

	private void startPlaying(Uri uri) {
		if (mPlayer == null) {
			mPlayer = new MediaPlayer();
		}
		try {
			mPlayer.setDataSource(mainActivity, uri);
			mPlayer.prepare();
			mPlayer.start();
			mPlayer.setOnCompletionListener(mp -> {
				mPlayer = null;
				if (isPlayingView != null) {
					((ImageView) isPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap
							(recordingBitmap);
					recordingBitmap = null;
					isPlayingView = null;
				}
			});
		} catch (IOException e) {
			Log.e(Constants.TAG, "prepare() failed", e);
			mainActivity.showMessage(R.string.error, ONStyle.ALERT);
		}
	}

	private void stopPlaying() {
		if (mPlayer != null) {
			if (isPlayingView != null) {
				((ImageView) isPlayingView.findViewById(R.id.gridview_item_picture)).setImageBitmap(recordingBitmap);
			}
			isPlayingView = null;
			recordingBitmap = null;
			mPlayer.release();
			mPlayer = null;
		}
	}

	private void startRecording(View v) {
		PermissionsHelper.requestPermission(getActivity(), Manifest.permission.RECORD_AUDIO,
				R.string.permission_audio_recording, snackBarPlaceholder, () -> {

					isRecording = true;
					toggleAudioRecordingStop(v);

					File f = StorageHelper.createNewAttachmentFile(mainActivity, Constants.MIME_TYPE_AUDIO_EXT);
					if (f == null) {
						mainActivity.showMessage(R.string.error, ONStyle.ALERT);
						return;
					}
					if (mRecorder == null) {
						mRecorder = new MediaRecorder();
						mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
						mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
						mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
						mRecorder.setAudioEncodingBitRate(96000);
						mRecorder.setAudioSamplingRate(44100);
					}
					recordName = f.getAbsolutePath();
					mRecorder.setOutputFile(recordName);

					try {
						audioRecordingTimeStart = Calendar.getInstance().getTimeInMillis();
						mRecorder.prepare();
						mRecorder.start();
					} catch (IOException | IllegalStateException e) {
						Log.e(Constants.TAG, "prepare() failed", e);
						mainActivity.showMessage(R.string.error, ONStyle.ALERT);
					}
				});
	}

	private void toggleAudioRecordingStop(View v) {
		if (isRecording) {
			((android.widget.TextView) v).setText(getString(R.string.stop));
			((android.widget.TextView) v).setTextColor(Color.parseColor("#ff0000"));
		}
	}

	private void stopRecording() {
		isRecording = false;
		if (mRecorder != null) {
			mRecorder.stop();
			audioRecordingTime = Calendar.getInstance().getTimeInMillis() - audioRecordingTimeStart;
			mRecorder.release();
			mRecorder = null;
		}
	}

	private void fade(final View v, boolean fadeIn) {

		int anim = R.animator.fade_out_support;
		int visibilityTemp = View.GONE;

		if (fadeIn) {
			anim = R.animator.fade_in_support;
			visibilityTemp = View.VISIBLE;
		}

		final int visibility = visibilityTemp;

		// Checks if user has left the app
		if (mainActivity != null) {
			Animation mAnimation = AnimationUtils.loadAnimation(mainActivity, anim);
			mAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					// Nothing to do
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
					// Nothing to do
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					v.setVisibility(visibility);
				}
			});
			v.startAnimation(mAnimation);
		}
	}

	TextLinkClickListener textLinkClickListener = new TextLinkClickListener() {
		@Override
		public void onTextLinkClick(View view, final String clickedString, final String url) {
			new MaterialDialog.Builder(mainActivity)
					.content(clickedString)
					.negativeColorRes(R.color.colorPrimary)
					.positiveText(R.string.open)
					.negativeText(R.string.copy)
					.callback(new MaterialDialog.ButtonCallback() {
						@Override
						public void onPositive(MaterialDialog dialog) {
							boolean error = false;
							Intent intent = null;
							try {
								intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
								intent.addCategory(Intent.CATEGORY_BROWSABLE);
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							} catch (NullPointerException e) {
								error = true;
							}

							if (intent == null
									|| error
									|| !IntentChecker
									.isAvailable(
											mainActivity,
											intent,
											new String[]{PackageManager.FEATURE_CAMERA})) {
								mainActivity.showMessage(R.string.no_application_can_perform_this_action,
										ONStyle.ALERT);

							} else {
								startActivity(intent);
							}
						}

						@Override
						public void onNegative(MaterialDialog dialog) {
							android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
									mainActivity
											.getSystemService(Activity.CLIPBOARD_SERVICE);
							android.content.ClipData clip = android.content.ClipData.newPlainText("text label",
									clickedString);
							clipboard.setPrimaryClip(clip);
						}
					}).build().show();
			View clickedView = noteTmp.isChecklist() ? toggleChecklistView : content;
			clickedView.clearFocus();
			KeyboardUtils.hideKeyboard(clickedView);
			new Handler().post(() -> {
				View clickedView1 = noteTmp.isChecklist() ? toggleChecklistView : content;
				KeyboardUtils.hideKeyboard(clickedView1);
			});
		}
	};

	@SuppressLint("NewApi")
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();

		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				Log.v(Constants.TAG, "MotionEvent.ACTION_DOWN");
				int w;

				Point displaySize = Display.getUsableSize(mainActivity);
				w = displaySize.x;

				if (x < Constants.SWIPE_MARGIN || x > w - Constants.SWIPE_MARGIN) {
					swiping = true;
					startSwipeX = x;
				}

				break;

			case MotionEvent.ACTION_UP:
				Log.v(Constants.TAG, "MotionEvent.ACTION_UP");
				if (swiping)
					swiping = false;
				break;

			case MotionEvent.ACTION_MOVE:
				if (swiping) {
					Log.v(Constants.TAG, "MotionEvent.ACTION_MOVE at position " + x + ", " + y);
					if (Math.abs(x - startSwipeX) > Constants.SWIPE_OFFSET) {
						swiping = false;
						FragmentTransaction transaction = mainActivity.getSupportFragmentManager().beginTransaction();
						mainActivity.animateTransition(transaction, mainActivity.TRANSITION_VERTICAL);
						DetailFragment mDetailFragment = new DetailFragment();
						Bundle b = new Bundle();
						b.putParcelable(Constants.INTENT_NOTE, new Note());
						mDetailFragment.setArguments(b);
						transaction.replace(R.id.fragment_container, mDetailFragment,
								mainActivity.FRAGMENT_DETAIL_TAG).addToBackStack(mainActivity
								.FRAGMENT_DETAIL_TAG).commit();
					}
				}
				break;

			default:
				Log.e(Constants.TAG, "Wrong element choosen: " + event.getAction());
		}

		return true;
	}

	@Override
	public void onAttachingFileErrorOccurred(Attachment mAttachment) {
		mainActivity.showMessage(R.string.error_saving_attachments, ONStyle.ALERT);
		if (noteTmp.getAttachmentsList().contains(mAttachment)) {
			removeAttachment(mAttachment);
			mAttachmentAdapter.notifyDataSetChanged();
			mGridView.autoresize();
		}
	}

	private void addAttachment(Attachment attachment) {
		noteTmp.addAttachment(attachment);
	}

	private void removeAttachment(Attachment mAttachment) {
		noteTmp.removeAttachment(mAttachment);
	}

	private void removeAttachment(int position) {
		noteTmp.removeAttachment(noteTmp.getAttachmentsList().get(position));
	}

	private void removeAllAttachments() {
		noteTmp.setAttachmentsList(new ArrayList<>());
		mAttachmentAdapter = new AttachmentAdapter(mainActivity, new ArrayList<>(), mGridView);
		mGridView.invalidateViews();
		mGridView.setAdapter(mAttachmentAdapter);
	}

	@Override
	public void onAttachingFileFinished(Attachment mAttachment) {
		addAttachment(mAttachment);
		mAttachmentAdapter.notifyDataSetChanged();
		mGridView.autoresize();
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		scrollContent();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// Nothing to do
	}

	@Override
	public void afterTextChanged(Editable s) {
		// Nothing to do
	}

	@Override
	public void onCheckListChanged() {
		scrollContent();
	}

	private void scrollContent() {
		if (noteTmp.isChecklist()) {
			if (mChecklistManager.getCount() > contentLineCounter) {
				scrollView.scrollBy(0, 60);
			}
			contentLineCounter = mChecklistManager.getCount();
		} else {
			if (content.getLineCount() > contentLineCounter) {
				scrollView.scrollBy(0, 60);
			}
			contentLineCounter = content.getLineCount();
		}
	}

	/**
	 * Add previously created tags to content
	 */
	private void addTags() {
		contentCursorPosition = getCursorIndex();

		// Retrieves all available categories
		final List<Tag> tags = TagsHelper.getAllTags();

		// If there is no tag a message will be shown
		if (tags.size() == 0) {
			mainActivity.showMessage(R.string.no_tags_created, ONStyle.WARN);
			return;
		}

		final Note currentNote = new Note();
		currentNote.setTitle(getNoteTitle());
		currentNote.setContent(getNoteContent());
		Integer[] preselectedTags = TagsHelper.getPreselectedTagsArray(currentNote, tags);

		// Dialog and events creation
		MaterialDialog dialog = new MaterialDialog.Builder(mainActivity)
				.title(R.string.select_tags)
				.positiveText(R.string.ok)
				.items(TagsHelper.getTagsArray(tags))
				.itemsCallbackMultiChoice(preselectedTags, (dialog1, which, text) -> {
					dialog1.dismiss();
					tagNote(tags, which, currentNote);
					return false;
				}).build();
		dialog.show();
	}

	private void tagNote(List<Tag> tags, Integer[] selectedTags, Note note) {
		Pair<String, List<Tag>> taggingResult = TagsHelper.addTagToNote(tags, selectedTags, note);

		StringBuilder sb;
		if (noteTmp.isChecklist()) {
			CheckListViewItem mCheckListViewItem = mChecklistManager.getFocusedItemView();
			if (mCheckListViewItem != null) {
				sb = new StringBuilder(mCheckListViewItem.getText());
				sb.insert(contentCursorPosition, " " + taggingResult.first + " ");
				mCheckListViewItem.setText(sb.toString());
				mCheckListViewItem.getEditText().setSelection(contentCursorPosition + taggingResult.first.length()
						+ 1);
			} else {
				title.append(" " + taggingResult.first);
			}
		} else {
			sb = new StringBuilder(getNoteContent());
			if (content.hasFocus()) {
				sb.insert(contentCursorPosition, " " + taggingResult.first + " ");
				content.setText(sb.toString());
				content.setSelection(contentCursorPosition + taggingResult.first.length() + 1);
			} else {
				if (getNoteContent().trim().length() > 0) {
					sb.append(System.getProperty("line.separator"))
							.append(System.getProperty("line.separator"));
				}
				sb.append(taggingResult.first);
				content.setText(sb.toString());
			}
		}

		// Removes unchecked tags
		if (taggingResult.second.size() > 0) {
			if (noteTmp.isChecklist()) {
				toggleChecklist2(true, true);
			}
			Pair<String, String> titleAndContent = TagsHelper.removeTag(getNoteTitle(), getNoteContent(),
					taggingResult.second);
			title.setText(titleAndContent.first);
			content.setText(titleAndContent.second);
			if (noteTmp.isChecklist()) {
				toggleChecklist2();
			}
		}
	}

	private int getCursorIndex() {
		if (!noteTmp.isChecklist()) {
			return content.getSelectionStart();
		} else {
			CheckListViewItem mCheckListViewItem = mChecklistManager.getFocusedItemView();
			if (mCheckListViewItem != null) {
				return mCheckListViewItem.getEditText().getSelectionStart();
			} else {
				return 0;
			}
		}
	}

	/**
	 * Used to check currently opened note from activity to avoid openind multiple times the same one
	 */
	public Note getCurrentNote() {
		return note;
	}

	private boolean isNoteLocationValid() {
		return noteTmp.getLatitude() != null
				&& noteTmp.getLatitude() != 0
				&& noteTmp.getLongitude() != null
				&& noteTmp.getLongitude() != 0;
	}

	/**
	 * Manages clicks on attachment dialog
	 */
	@SuppressLint("InlinedApi")
	private class AttachmentOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {

			switch (v.getId()) {
				// Photo from camera
				case R.id.camera:
					takePhoto();
					break;
				case R.id.recording:
					if (!isRecording) {
						startRecording(v);
					} else {
						stopRecording();
						Attachment attachment = new Attachment(Uri.fromFile(new File(recordName)), Constants
								.MIME_TYPE_AUDIO);
						attachment.setLength(audioRecordingTime);
						addAttachment(attachment);
						mAttachmentAdapter.notifyDataSetChanged();
						mGridView.autoresize();
					}
					break;
				case R.id.video:
					takeVideo();
					break;
				case R.id.sketch:
					takeSketch(null);
					break;
				case R.id.location:
					displayLocationDialog();
					break;
				case R.id.timestamp:
					addTimestamp();
					break;
				default:
					Log.e(Constants.TAG, "Wrong element choosen: " + v.getId());
			}
			if (!isRecording) attachmentDialog.dismiss();
		}
	}

	public void startGetContentAction() {
		Intent filesIntent;
		filesIntent = new Intent(Intent.ACTION_GET_CONTENT);
		filesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
		filesIntent.addCategory(Intent.CATEGORY_OPENABLE);
		filesIntent.setType("*/*");
		startActivityForResult(filesIntent, FILES);
	}

	private void askReadExternalStoragePermission() {
		PermissionsHelper.requestPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE,
				R.string.permission_external_storage_detail_attachment,
				snackBarPlaceholder, this::startGetContentAction);
	}

	public void onEventMainThread(PushbulletReplyEvent pushbulletReplyEvent) {
		String text = getNoteContent() + System.getProperty("line.separator") + pushbulletReplyEvent.message;
		content.setText(text);
	}
}



