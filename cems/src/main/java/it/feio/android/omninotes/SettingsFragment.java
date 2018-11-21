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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import it.feio.android.analitica.AnalyticsHelper;
import it.feio.android.omninotes.async.DataBackupIntentService;
import it.feio.android.omninotes.helpers.AppVersionHelper;
import it.feio.android.omninotes.helpers.LanguageHelper;
import it.feio.android.omninotes.helpers.PermissionsHelper;
import it.feio.android.omninotes.models.ONStyle;
import it.feio.android.omninotes.utils.*;
import org.apache.commons.lang.StringUtils;
import com.epson.epos2.printer.Printer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


public class SettingsFragment extends PreferenceFragment {

	private SharedPreferences prefs;

	private final int SPRINGPAD_IMPORT = 0;
	private final int RINGTONE_REQUEST_CODE = 100;
	public final static String XML_NAME = "xmlName";
	private Activity activity;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int xmlId = R.xml.settings;
		if (getArguments() != null && getArguments().containsKey(XML_NAME)) {
			xmlId = ResourcesUtils.getXmlId(OmniNotes.getAppContext(), ResourcesUtils.ResourceIdentifiers.xml, String
					.valueOf(getArguments().get(XML_NAME)));
		}
		addPreferencesFromResource(xmlId);
	}


	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.activity = activity;
		prefs = activity.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_MULTI_PROCESS);
		setTitle();
	}


	private void setTitle() {
		String title = getString(R.string.settings);
		if (getArguments() != null && getArguments().containsKey(XML_NAME)) {
			String xmlName = getArguments().getString(XML_NAME);
			if (!TextUtils.isEmpty(xmlName)) {
				int stringResourceId = getActivity().getResources().getIdentifier(xmlName.replace("settings_",
						"settings_screen_"), "string", getActivity().getPackageName());
				title = stringResourceId != 0 ? getString(stringResourceId) : title;
			}
		}
		Toolbar toolbar = ((Toolbar) getActivity().findViewById(R.id.toolbar));
		if (toolbar != null) toolbar.setTitle(title);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				getActivity().onBackPressed();
				break;
			default:
				Log.e(Constants.TAG, "Wrong element choosen: " + item.getItemId());
		}
		return super.onOptionsItemSelected(item);
	}


	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);
		if (preference instanceof PreferenceScreen) {
			((SettingsActivity) getActivity()).switchToScreen(preference.getKey());
		}
		return false;
	}


	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		super.onResume();

		// Export notes
		Preference export = findPreference("settings_export_data");
		if (export != null) {
			export.setOnPreferenceClickListener(arg0 -> {

				// Inflate layout
				LayoutInflater inflater = getActivity().getLayoutInflater();
				View v = inflater.inflate(R.layout.dialog_backup_layout, null);

				// Finds actually saved backups names
				PermissionsHelper.requestPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, R
						.string.permission_external_storage, activity.findViewById(R.id.crouton_handle), () -> export
						(v));

				return false;
			});
		}

		// Import notes
		Preference importData = findPreference("settings_import_data");
		if (importData != null) {
			importData.setOnPreferenceClickListener(arg0 -> {
				PermissionsHelper.requestPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, R
						.string.permission_external_storage, activity.findViewById(R.id.crouton_handle), () -> importNotes());
				return false;
			});
		}


		// Import notes from Springpad export zip file
		Preference importFromSpringpad = findPreference("settings_import_from_springpad");
		if (importFromSpringpad != null) {
			importFromSpringpad.setOnPreferenceClickListener(arg0 -> {
				Intent intent;
				intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("application/zip");
				if (!IntentChecker.isAvailable(getActivity(), intent, null)) {
					Toast.makeText(getActivity(), R.string.feature_not_available_on_this_device,
							Toast.LENGTH_SHORT).show();
					return false;
				}
				startActivityForResult(intent, SPRINGPAD_IMPORT);
				return false;
			});
		}


//		Preference syncWithDrive = findPreference("settings_backup_drive");
//		importFromSpringpad.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			@Override
//			public boolean onPreferenceClick(Preference arg0) {
//				Intent intent;
//				intent = new Intent(Intent.ACTION_GET_CONTENT);
//				intent.addCategory(Intent.CATEGORY_OPENABLE);
//				intent.setType("application/zip");
//				if (!IntentChecker.isAvailable(getActivity(), intent, null)) {
//					Crouton.makeText(getActivity(), R.string.feature_not_available_on_this_device,
// ONStyle.ALERT).show();
//					return false;
//				}
//				startActivityForResult(intent, SPRINGPAD_IMPORT);
//				return false;
//			}
//		});


		// Swiping action
		final SwitchPreference swipeToTrash = (SwitchPreference) findPreference("settings_swipe_to_trash");
		if (swipeToTrash != null) {
			if (prefs.getBoolean("settings_swipe_to_trash", false)) {
				swipeToTrash.setChecked(true);
				swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_2));
			} else {
				swipeToTrash.setChecked(false);
				swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_1));
			}
			swipeToTrash.setOnPreferenceChangeListener((preference, newValue) -> {
				if ((Boolean) newValue) {
					swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_2));
				} else {
					swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_1));
				}
				return true;
			});
		}


		// Show uncategorized notes in menu
		final SwitchPreference showUncategorized = (SwitchPreference) findPreference(Constants
				.PREF_SHOW_UNCATEGORIZED);
		if (showUncategorized != null) {
			showUncategorized.setOnPreferenceChangeListener((preference, newValue) -> {
				return true;
			});
		}


		// Show Automatically adds location to new notes
		final SwitchPreference autoLocation = (SwitchPreference) findPreference(Constants.PREF_AUTO_LOCATION);
		if (autoLocation != null) {
			autoLocation.setOnPreferenceChangeListener((preference, newValue) -> {
				return true;
			});
		}


		// Maximum video attachment size
		final EditTextPreference maxVideoSize = (EditTextPreference) findPreference("settings_max_video_size");
		if (maxVideoSize != null) {
			String maxVideoSizeValue = prefs.getString("settings_max_video_size", getString(R.string.not_set));
			maxVideoSize.setSummary(getString(R.string.settings_max_video_size_summary) + ": " + String.valueOf
					(maxVideoSizeValue));
			maxVideoSize.setOnPreferenceChangeListener((preference, newValue) -> {
				maxVideoSize.setSummary(getString(R.string.settings_max_video_size_summary) + ": " + String
						.valueOf(newValue));
				prefs.edit().putString("settings_max_video_size", newValue.toString()).commit();
				return false;
			});
		}


		// Set notes' protection password
		Preference password = findPreference("settings_password");
		if (password != null) {
			password.setOnPreferenceClickListener(preference -> {
				Intent passwordIntent = new Intent(getActivity(), PasswordActivity.class);
				startActivity(passwordIntent);
				return false;
			});
		}


		// Use password to grant application access
		final SwitchPreference passwordAccess = (SwitchPreference) findPreference("settings_password_access");
		if (passwordAccess != null) {
			if (prefs.getString(Constants.PREF_PASSWORD, null) == null) {
				passwordAccess.setEnabled(false);
				passwordAccess.setChecked(false);
			} else {
				passwordAccess.setEnabled(true);
			}
			passwordAccess.setOnPreferenceChangeListener((preference, newValue) -> {
				PasswordHelper.requestPassword(getActivity(), passwordConfirmed -> {
					if (passwordConfirmed) {
						passwordAccess.setChecked((Boolean) newValue);
					}
				});
				return true;
			});
		}


		// Languages
		ListPreference lang = (ListPreference) findPreference("settings_language");
		if (lang != null) {
			String languageName = getResources().getConfiguration().locale.getDisplayName();
			lang.setSummary(languageName.substring(0, 1).toUpperCase(getResources().getConfiguration().locale)
					+ languageName.substring(1, languageName.length()));
			lang.setOnPreferenceChangeListener((preference, value) -> {
				LanguageHelper.updateLanguage(getActivity(), value.toString());
				SystemHelper.restartApp(getActivity().getApplicationContext(), MainActivity.class);
				return false;
			});
		}


		// Text size
		final ListPreference textSize = (ListPreference) findPreference("settings_text_size");
		if (textSize != null) {
			int textSizeIndex = textSize.findIndexOfValue(prefs.getString("settings_text_size", "default"));
			String textSizeString = getResources().getStringArray(R.array.text_size)[textSizeIndex];
			textSize.setSummary(textSizeString);
			textSize.setOnPreferenceChangeListener((preference, newValue) -> {
				int textSizeIndex1 = textSize.findIndexOfValue(newValue.toString());
				String checklistString = getResources().getStringArray(R.array.text_size)[textSizeIndex1];
				textSize.setSummary(checklistString);
				prefs.edit().putString("settings_text_size", newValue.toString()).commit();
				textSize.setValueIndex(textSizeIndex1);
				return false;
			});
		}


		// Application's colors
		final ListPreference colorsApp = (ListPreference) findPreference("settings_colors_app");
		if (colorsApp != null) {
			int colorsAppIndex = colorsApp.findIndexOfValue(prefs.getString("settings_colors_app",
					Constants.PREF_COLORS_APP_DEFAULT));
			String colorsAppString = getResources().getStringArray(R.array.colors_app)[colorsAppIndex];
			colorsApp.setSummary(colorsAppString);
			colorsApp.setOnPreferenceChangeListener((preference, newValue) -> {
				int colorsAppIndex1 = colorsApp.findIndexOfValue(newValue.toString());
				String colorsAppString1 = getResources().getStringArray(R.array.colors_app)[colorsAppIndex1];
				colorsApp.setSummary(colorsAppString1);
				prefs.edit().putString("settings_colors_app", newValue.toString()).commit();
				colorsApp.setValueIndex(colorsAppIndex1);
				return false;
			});
		}


		// Checklists
		final ListPreference checklist = (ListPreference) findPreference("settings_checked_items_behavior");
		if (checklist != null) {
			int checklistIndex = checklist.findIndexOfValue(prefs.getString("settings_checked_items_behavior", "0"));
			String checklistString = getResources().getStringArray(R.array.checked_items_behavior)[checklistIndex];
			checklist.setSummary(checklistString);
			checklist.setOnPreferenceChangeListener((preference, newValue) -> {
				int checklistIndex1 = checklist.findIndexOfValue(newValue.toString());
				String checklistString1 = getResources().getStringArray(R.array.checked_items_behavior)
						[checklistIndex1];
				checklist.setSummary(checklistString1);
				prefs.edit().putString("settings_checked_items_behavior", newValue.toString()).commit();
				checklist.setValueIndex(checklistIndex1);
				return false;
			});
		}


		// Widget's colors
		final ListPreference colorsWidget = (ListPreference) findPreference("settings_colors_widget");
		if (colorsWidget != null) {
			int colorsWidgetIndex = colorsWidget.findIndexOfValue(prefs.getString("settings_colors_widget",
					Constants.PREF_COLORS_APP_DEFAULT));
			String colorsWidgetString = getResources().getStringArray(R.array.colors_widget)[colorsWidgetIndex];
			colorsWidget.setSummary(colorsWidgetString);
			colorsWidget.setOnPreferenceChangeListener((preference, newValue) -> {
				int colorsWidgetIndex1 = colorsWidget.findIndexOfValue(newValue.toString());
				String colorsWidgetString1 = getResources().getStringArray(R.array.colors_widget)[colorsWidgetIndex1];
				colorsWidget.setSummary(colorsWidgetString1);
				prefs.edit().putString("settings_colors_widget", newValue.toString()).commit();
				colorsWidget.setValueIndex(colorsWidgetIndex1);
				return false;
			});
		}

		// Notification snooze delay
		final EditTextPreference snoozeDelay = (EditTextPreference) findPreference
				("settings_notification_snooze_delay");
		if (snoozeDelay != null) {
			String snooze = prefs.getString("settings_notification_snooze_delay", Constants.PREF_SNOOZE_DEFAULT);
			snooze = TextUtils.isEmpty(snooze) ? Constants.PREF_SNOOZE_DEFAULT : snooze;
			snoozeDelay.setSummary(String.valueOf(snooze) + " " + getString(R.string.minutes));
			snoozeDelay.setOnPreferenceChangeListener((preference, newValue) -> {
				String snoozeUpdated = TextUtils.isEmpty(String.valueOf(newValue)) ? Constants
						.PREF_SNOOZE_DEFAULT : String.valueOf(newValue);
				snoozeDelay.setSummary(snoozeUpdated + " " + getString(R.string.minutes));
				prefs.edit().putString("settings_notification_snooze_delay", snoozeUpdated).apply();
				return false;
			});
		}


		// NotificationServiceListener shortcut
		final Preference norificationServiceListenerPreference = findPreference("settings_notification_service_listener");
		if (norificationServiceListenerPreference != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
				getPreferenceScreen().removePreference(norificationServiceListenerPreference);
			}
		}


		Preference cg1 = findPreference("settings_ca_chamber");
		if (cg1 != null) {
			cg1.setOnPreferenceClickListener(arg0 -> {

				new MaterialDialog.Builder(getContext())
						.title("CA Chamber")
						.content("가스지문 현출기이며 지문현출에 필요한 가성소다 및 접착제 가스를 사용하는 방법이다.\n" +
								"가로세로 60cm 남짓한 밀폐된 chamber는 감식요원들이 유해가스를 마실 위험이 없고, 짧은 시간 안에 지문이 현출되는 장점이 있다.")
						.positiveText(R.string.ok)
						.build().show();
				return false;
			});
		}
		Preference cg2 = findPreference("settings_digital_camera");
		if (cg2 != null) {
			cg2.setOnPreferenceClickListener(arg0 -> {

				new MaterialDialog.Builder(getContext())
						.title("디지털 비디오 카메라 (CCTV)")
						.content("증거물의 외형을 사진/영상으로 찍는다. 더 나아가 범죄현장에 설치된 CCTV는 아주 중요한 증거가 된다.")
						.positiveText(R.string.ok)
						.build().show();
				return false;
			});
		}
		Preference cg3 = findPreference("settings_cotton_bud");
		if (cg3 != null) {
			cg3.setOnPreferenceClickListener(arg0 -> {

				new MaterialDialog.Builder(getContext())
						.title("멸균면봉")
						.content("주로 적은 양의 유체를 증거물로써 채집할 때 쓰인다. 성폭력 사건에서의 적은양의 타액, 정액, 혈흔 등과 더불어 주변 환경에서 증거를 채집할 때 쓰인다.")
						.positiveText(R.string.ok)
						.build().show();
				return false;
			});
		}
		Preference cg4 = findPreference("settings_forensic_light");
		if (cg4 != null) {
			cg4.setOnPreferenceClickListener(arg0 -> {

				new MaterialDialog.Builder(getContext())
						.title("법광원 (Forensic Light Source)")
						.content("감식용 LED 조명이다. 과학수사 활동에 사용되는 플래시 광원을 일반적으로 푸른색 법광원을 사용한다. 형광 반응에 간섭하지 않도록 목표 파장보다\n" +
								"장파장 대역을 차단한 경우가 일반적인 빛이다.\n" +
								"이를 통해 소변, 정액, 혈흔 등을 보다 넓은 범위에서 확인할 수 있다.\n" +
								"기술의 발달로, 파장대별로 셋팅이 가능함은 물론 밝기조절 등을 할 수 있는 휴대용 가변광원장비도 사용되고있다.")
						.positiveText(R.string.ok)
						.build().show();
				return false;
			});
		}
		Preference cg5 = findPreference("settings_fingerprint");
		if (cg5 != null) {
			cg5.setOnPreferenceClickListener(arg0 -> {

				new MaterialDialog.Builder(getContext())
						.title("분말법, 지문현출용액")
						.content("분말법은 고체법이라고도 불린다. 지문이 인상되었다고 생각되는 물체나 부분에 미세한 분말을 도포하여 잠재적 지문을 검출하는 방법이다. \n" +
								"주로 표면이 편평하고 매끄로우며 경질의 물체상에 유류된 잠재지문을 채취하는데 적당하다.\n" +
								"화장품 가루 기술을 적용하여 보다 편리한 S분말 등이 사용된다.\n" +
								"지문현출용액을 사용하는 방법은 액체법이라고도 불린다. 증거물의 염분이나 단백질 등에 화학적 반응을 일으켜서 지문을 검출하는 방법으로, \n" +
								"닌히드린 용액법과 초산은 용액법 등이 있다. 주로 지류에서 지문을 검출하는 경우에 사용한다.")
						.positiveText(R.string.ok)
						.build().show();
				return false;
			});
		}
		Preference cg6 = findPreference("settings_footprint");
		if (cg6 != null) {
			cg6.setOnPreferenceClickListener(arg0 -> {

				new MaterialDialog.Builder(getContext())
						.title("정전기 족적 채취")
						.content("사진 및 육안으로 확인이 안되는 족적을 채취하는데에 쓰인다. 이불이나 의류 등에 남겨진 족적을 정전기의 원리를 이용하여 채집하는 방법이다. \n" +
								"센 전압을 가해주어 정전기를 일으켜 금속판에 족적의 무늬가 그대로 달라붙는 휴대용 감식 장비이다.")
						.positiveText(R.string.ok)
						.build().show();
				return false;
			});
		}
		Preference cg7 = findPreference("settings_microscope");
		if (cg7 != null) {
			cg7.setOnPreferenceClickListener(arg0 -> {

				new MaterialDialog.Builder(getContext())
						.title("현미경")
						.content("증거물 채집 후, 육안으로 보이지 않는 물질을 세밀하게 분석할 때 사용된다. 현미경의 분해능에 따라 증거물이 확대되어 보인다.")
						.positiveText(R.string.ok)
						.build().show();
				return false;
			});
		}


		// Settings reset
		Preference resetData = findPreference("reset_all_data");
		if (resetData != null) {
			resetData.setOnPreferenceClickListener(arg0 -> {

				new MaterialDialog.Builder(activity)
						.content(R.string.reset_all_data_confirmation)
						.positiveText(R.string.confirm)
						.callback(new MaterialDialog.ButtonCallback() {
							@Override
							public void onPositive(MaterialDialog dialog) {
								prefs.edit().clear().commit();
								File db = getActivity().getDatabasePath(Constants.DATABASE_NAME);
								StorageHelper.delete(getActivity(), db.getAbsolutePath());
								File attachmentsDir = StorageHelper.getAttachmentDir(getActivity());
								StorageHelper.delete(getActivity(), attachmentsDir.getAbsolutePath());
								File cacheDir = StorageHelper.getCacheDir(getActivity());
								StorageHelper.delete(getActivity(), cacheDir.getAbsolutePath());
								SystemHelper.restartApp(getActivity().getApplicationContext(), MainActivity.class);
							}
						})
						.build().show();

				return false;
			});
		}


		// Instructions
		Preference instructions = findPreference("settings_tour_show_again");
		if (instructions != null) {
			instructions.setOnPreferenceClickListener(arg0 -> {
				new MaterialDialog.Builder(getActivity())
						.content(getString(R.string.settings_tour_show_again_summary) + "?")
						.positiveText(R.string.confirm)
						.callback(new MaterialDialog.ButtonCallback() {
							@Override
							public void onPositive(MaterialDialog materialDialog) {

								((OmniNotes)getActivity().getApplication()).getAnalyticsHelper().trackEvent(AnalyticsHelper.CATEGORIES.SETTING, "settings_tour_show_again");

								prefs.edit().putBoolean(Constants.PREF_TOUR_COMPLETE, false).commit();
								SystemHelper.restartApp(getActivity().getApplicationContext(), MainActivity.class);
							}
						}).build().show();
				return false;
			});
		}


		// Donations
//        Preference donation = findPreference("settings_donation");
//        if (donation != null) {
//            donation.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//                @Override
//                public boolean onPreferenceClick(Preference preference) {
//                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
//
//                    ArrayList<ImageAndTextItem> options = new ArrayList<ImageAndTextItem>();
//                    options.add(new ImageAndTextItem(R.drawable.ic_paypal, getString(R.string.paypal)));
//                    options.add(new ImageAndTextItem(R.drawable.ic_bitcoin, getString(R.string.bitcoin)));
//
//                    alertDialogBuilder
//                            .setAdapter(new ImageAndTextAdapter(getActivity(), options),
//                                    new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            switch (which) {
//                                                case 0:
//                                                    Intent intentPaypal = new Intent(Intent.ACTION_VIEW);
//                                                    intentPaypal.setData(Uri.parse(getString(R.string.paypal_url)));
//                                                    startActivity(intentPaypal);
//                                                    break;
//                                                case 1:
//                                                    Intent intentBitcoin = new Intent(Intent.ACTION_VIEW);
//                                                    intentBitcoin.setData(Uri.parse(getString(R.string.bitcoin_url)));
//                                                    startActivity(intentBitcoin);
//                                                    break;
//                                            }
//                                        }
//                                    });
//
//
//                    // create alert dialog
//                    AlertDialog alertDialog = alertDialogBuilder.create();
//                    // show it
//                    alertDialog.show();
//                    return false;
//                }
//            });
//        }
	}


	private void importNotes() {
		final CharSequence[] backups = StorageHelper.getExternalStoragePublicDir().list();

		if (backups != null && backups.length == 0) {
			((SettingsActivity)getActivity()).showMessage(R.string.no_backups_available, ONStyle.WARN);
		} else {

			MaterialDialog importDialog = new MaterialDialog.Builder(getActivity())
					.title(R.string.data_import_message)
					.items(backups)
					.positiveText(R.string.confirm)
					.callback(new MaterialDialog.ButtonCallback() {
						@Override
						public void onPositive(MaterialDialog materialDialog) {

						}
					}).build();

			// OnShow is overridden to allow long-click on item so user can remove them
			importDialog.setOnShowListener(dialog -> {

				ListView lv = importDialog.getListView();
				assert lv != null;
				lv.setOnItemClickListener((parent, view, position, id) -> {

					// Retrieves backup size
					File backupDir = StorageHelper.getBackupDir(backups[position].toString());
					long size = StorageHelper.getSize(backupDir) / 1024;
					String sizeString = size > 1024 ? size / 1024 + "Mb" : size + "Kb";

					// Check preference presence
					String prefName = StorageHelper.getSharedPreferencesFile(getActivity()).getName();
					boolean hasPreferences = (new File(backupDir, prefName)).exists();

					String message = backups[position]
							+ " (" + sizeString
							+ (hasPreferences ? " " + getString(R.string.settings_included) : "")
							+ ")";

					new MaterialDialog.Builder(getActivity())
							.title(R.string.confirm_restoring_backup)
							.content(message)
							.positiveText(R.string.confirm)
							.callback(new MaterialDialog.ButtonCallback() {
								@Override
								public void onPositive(MaterialDialog materialDialog) {

									((OmniNotes)getActivity().getApplication()).getAnalyticsHelper().trackEvent(AnalyticsHelper.CATEGORIES.SETTING,
											"settings_import_data");

									importDialog.dismiss();

									// An IntentService will be launched to accomplish the import task
									Intent service = new Intent(getActivity(),
											DataBackupIntentService.class);
									service.setAction(DataBackupIntentService.ACTION_DATA_IMPORT);
									service.putExtra(DataBackupIntentService.INTENT_BACKUP_NAME,
											backups[position]);
									getActivity().startService(service);
								}
							}).build().show();
				});

				// Creation of backup removal dialog
				lv.setOnItemLongClickListener((parent, view, position, id) -> {

					// Retrieves backup size
					File backupDir = StorageHelper.getBackupDir(backups[position].toString());
					long size = StorageHelper.getSize(backupDir) / 1024;
					String sizeString = size > 1024 ? size / 1024 + "Mb" : size + "Kb";

					new MaterialDialog.Builder(getActivity())
							.title(R.string.confirm_removing_backup)
							.content(backups[position] + "" + " (" + sizeString + ")")
							.positiveText(R.string.confirm)
							.callback(new MaterialDialog.ButtonCallback() {
								@Override
								public void onPositive(MaterialDialog materialDialog) {
									importDialog.dismiss();
									// An IntentService will be launched to accomplish the deletion task
									Intent service = new Intent(getActivity(),
											DataBackupIntentService.class);
									service.setAction(DataBackupIntentService.ACTION_DATA_DELETE);
									service.putExtra(DataBackupIntentService.INTENT_BACKUP_NAME,
											backups[position]);
									getActivity().startService(service);
								}
							}).build().show();

					return true;
				});
			});

			importDialog.show();
		}
	}


	private void export(View v) {
		final List<String> backups = Arrays.asList(StorageHelper.getExternalStoragePublicDir().list());

		// Sets default export file name
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_EXPORT);
		String fileName = sdf.format(Calendar.getInstance().getTime());
		final EditText fileNameEditText = (EditText) v.findViewById(R.id.export_file_name);
		final TextView backupExistingTextView = (TextView) v.findViewById(R.id.backup_existing);
		fileNameEditText.setHint(fileName);
		fileNameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
			@Override
			public void afterTextChanged(Editable arg0) {
				if (backups.contains(arg0.toString())) {
					backupExistingTextView.setText(R.string.backup_existing);
				} else {
					backupExistingTextView.setText("");
				}
			}
		});

		new MaterialDialog.Builder(getActivity())
				.title(R.string.data_export_message)
				.customView(v, false)
				.positiveText(R.string.confirm)
				.callback(new MaterialDialog.ButtonCallback() {
					@Override
					public void onPositive(MaterialDialog materialDialog) {
						((OmniNotes)getActivity().getApplication()).getAnalyticsHelper().trackEvent(AnalyticsHelper.CATEGORIES.SETTING, "settings_export_data");
						// An IntentService will be launched to accomplish the export task
						Intent service = new Intent(getActivity(), DataBackupIntentService.class);
						service.setAction(DataBackupIntentService.ACTION_DATA_EXPORT);
						String backupName = StringUtils.isEmpty(fileNameEditText.getText().toString()) ?
								fileNameEditText.getHint().toString() : fileNameEditText.getText().toString();
						service.putExtra(DataBackupIntentService.INTENT_BACKUP_NAME, backupName);
						getActivity().startService(service);
					}
				}).build().show();
	}


	@Override
	public void onStart() {
		((OmniNotes)getActivity().getApplication()).getAnalyticsHelper().trackScreenView(getClass().getName());
		super.onStart();
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
				case SPRINGPAD_IMPORT:
					Uri filesUri = intent.getData();
					String path = FileHelper.getPath(getActivity(), filesUri);
					// An IntentService will be launched to accomplish the import task
					Intent service = new Intent(getActivity(), DataBackupIntentService.class);
					service.setAction(DataBackupIntentService.ACTION_DATA_IMPORT_SPRINGPAD);
					service.putExtra(DataBackupIntentService.EXTRA_SPRINGPAD_BACKUP, path);
					getActivity().startService(service);
					break;

				case RINGTONE_REQUEST_CODE:
					Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
					String notificationSound = uri == null ? null : uri.toString();
					prefs.edit().putString("settings_notification_ringtone", notificationSound).apply();
					break;

				default:
					Log.e(Constants.TAG, "Wrong element choosen: " + requestCode);
			}
		}
	}
}
