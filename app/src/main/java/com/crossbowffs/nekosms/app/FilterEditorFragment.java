package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.crossbowffs.nekosms.filters.RegexFilterPattern;
import com.crossbowffs.nekosms.filters.StringFilterPattern;
import com.google.android.material.textfield.TextInputLayout;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;
import com.crossbowffs.nekosms.utils.MapUtils;
import com.crossbowffs.nekosms.widget.EnumAdapter;
import com.crossbowffs.nekosms.widget.OnItemSelectedListenerAdapter;
import com.crossbowffs.nekosms.widget.TextWatcherAdapter;

import java.util.HashMap;
import java.util.Map;

public class FilterEditorFragment extends Fragment {
    private enum CaseSensitivity {
        INSENSITIVE,
        SENSITIVE;

        public static CaseSensitivity fromBoolean(boolean caseSensitive) {
            return caseSensitive ? SENSITIVE : INSENSITIVE;
        }

        public boolean toBoolean() {
            return this == SENSITIVE;
        }
    }

    public static final String EXTRA_FIELD = "field";

    private SmsFilterField mField;
    private TextInputLayout mPatternTextInputLayout;
    private EditText mPatternEditText;
    private Spinner mModeSpinner;
    private Spinner mCaseSpinner;
    private EnumAdapter<SmsFilterMode> mModeAdapter;
    private EnumAdapter<CaseSensitivity> mCaseAdapter;
    private SmsFilterPatternData mPatternData;
    private EditText mTestText;
    private Button mTestButton;

    private FilterEditorActivity getEditorActivity() {
        return (FilterEditorActivity)getActivity();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mField = (SmsFilterField)getArguments().getSerializable(EXTRA_FIELD);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pattern_editor, container, false);
        mPatternTextInputLayout = (TextInputLayout)view.findViewById(R.id.filter_editor_pattern_inputlayout);
        mPatternEditText = (EditText)view.findViewById(R.id.filter_editor_pattern_edittext);
        mModeSpinner = (Spinner)view.findViewById(R.id.filter_editor_mode_spinner);
        mCaseSpinner = (Spinner)view.findViewById(R.id.filter_editor_case_spinner);
        mTestText = (EditText)view.findViewById(R.id.filter_editor_pattern_test_text);
        mTestButton = (Button)view.findViewById(R.id.filter_editor_pattern_test_button);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up spinner adapters
        mModeAdapter = new EnumAdapter<>(getEditorActivity(), android.R.layout.simple_spinner_dropdown_item, SmsFilterMode.class);
        mModeAdapter.setStringMap(getModeMap());
        mModeSpinner.setAdapter(mModeAdapter);

        mCaseAdapter = new EnumAdapter<>(getEditorActivity(), android.R.layout.simple_spinner_dropdown_item, CaseSensitivity.class);
        mCaseAdapter.setStringMap(getCaseMap());
        mCaseSpinner.setAdapter(mCaseAdapter);

        // Load pattern data corresponding to the current tab
        mPatternData = getEditorActivity().getPatternData(mField);

        // Disable hint animation as workaround for drawing issue during activity creation
        // See https://code.google.com/p/android/issues/detail?id=179776
        mPatternTextInputLayout.setHintAnimationEnabled(false);
        mPatternEditText.setText(mPatternData.getPattern());
        mPatternTextInputLayout.setHintAnimationEnabled(true);
        mPatternEditText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                mPatternData.setPattern(s.toString());
            }
        });

        mModeSpinner.setSelection(mModeAdapter.getPosition(mPatternData.getMode()));
        mModeSpinner.setOnItemSelectedListener(new OnItemSelectedListenerAdapter() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPatternData.setMode(mModeAdapter.getItem(position));
            }
        });

        mCaseSpinner.setSelection(mCaseAdapter.getPosition(CaseSensitivity.fromBoolean(mPatternData.isCaseSensitive())));
        mCaseSpinner.setOnItemSelectedListener(new OnItemSelectedListenerAdapter() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPatternData.setCaseSensitive(mCaseAdapter.getItem(position).toBoolean());
            }
        });

        // Bind click event
        mTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testPattern();
            }
        });
    }

    private Map<SmsFilterMode, String> getModeMap() {
        Resources resources = getResources();
        HashMap<SmsFilterMode, String> modeMap = MapUtils.hashMapForSize(6);
        modeMap.put(SmsFilterMode.REGEX, resources.getString(R.string.filter_mode_regex));
        modeMap.put(SmsFilterMode.WILDCARD, resources.getString(R.string.filter_mode_wildcard));
        modeMap.put(SmsFilterMode.CONTAINS, resources.getString(R.string.filter_mode_contains));
        modeMap.put(SmsFilterMode.PREFIX, resources.getString(R.string.filter_mode_prefix));
        modeMap.put(SmsFilterMode.SUFFIX, resources.getString(R.string.filter_mode_suffix));
        modeMap.put(SmsFilterMode.EQUALS, resources.getString(R.string.filter_mode_equals));
        return modeMap;
    }

    private Map<CaseSensitivity, String> getCaseMap() {
        Resources resources = getResources();
        HashMap<CaseSensitivity, String> caseMap = MapUtils.hashMapForSize(2);
        caseMap.put(CaseSensitivity.INSENSITIVE, resources.getString(R.string.filter_case_insensitive));
        caseMap.put(CaseSensitivity.SENSITIVE, resources.getString(R.string.filter_case_sensitive));
        return caseMap;
    }

    private void testPattern() {
        String text = mTestText.getText().toString();
        boolean matches = false;

        if (mPatternData.hasData()) {
            switch (mPatternData.getMode()) {
                case REGEX:
                case WILDCARD:
                    FilterEditorActivity filterEditorActivity = (FilterEditorActivity) getActivity();
                    String patternError = filterEditorActivity.validatePatternString(mPatternData);
                    if (patternError != null) {
                        filterEditorActivity.showInvalidPatternDialog(patternError);
                        return;
                    } else {
                        RegexFilterPattern regexFilter = new RegexFilterPattern(mPatternData);
                        matches = regexFilter.match(text, text);
                    }
                    break;
                case CONTAINS:
                case PREFIX:
                case SUFFIX:
                case EQUALS:
                    StringFilterPattern Stringfilter = new StringFilterPattern(mPatternData);
                    matches = Stringfilter.match(text, text);
                    break;
            }
        }

        if (matches) {
            showPatternTestResult(text + "\n\n" + getString(R.string.filter_pattern_test_hit));
        } else {
            showPatternTestResult(text + "\n\n" + getString(R.string.filter_pattern_test_not_hit));
        }
    }

    private void showPatternTestResult(String testResult) {
        new AlertDialog.Builder(getActivity())
		    .setMessage(testResult)
            .setPositiveButton(R.string.ok, null)
            .show();
    }
}
