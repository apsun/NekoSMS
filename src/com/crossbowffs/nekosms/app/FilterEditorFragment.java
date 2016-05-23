package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;
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

    public static final String EXTRA_MODE = "mode";
    public static final int EXTRA_MODE_SENDER = 0;
    public static final int EXTRA_MODE_BODY = 1;

    private EditText mPatternEditText;
    private Spinner mModeSpinner;
    private Spinner mCaseSpinner;
    private EnumAdapter<SmsFilterMode> mModeAdapter;
    private EnumAdapter<CaseSensitivity> mCaseAdapter;
    private SmsFilterPatternData mPatternData;

    private FilterEditorActivity getEditorActivity() {
        return (FilterEditorActivity)getActivity();
    }

    private SmsFilterField getFilterField() {
        int modeCode = getArguments().getInt(EXTRA_MODE);
        switch (modeCode) {
        case EXTRA_MODE_SENDER:
            return SmsFilterField.SENDER;
        case EXTRA_MODE_BODY:
            return SmsFilterField.BODY;
        default:
            return null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pattern_editor, container, false);
        mPatternEditText = (EditText)view.findViewById(R.id.filter_editor_pattern_edittext);
        mModeSpinner = (Spinner)view.findViewById(R.id.filter_editor_mode_spinner);
        mCaseSpinner = (Spinner)view.findViewById(R.id.filter_editor_case_spinner);
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
        mPatternData = getEditorActivity().getPatternData(getFilterField());

        // Bind controls to pattern fields
        mPatternEditText.setText(mPatternData.getPattern());
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
    }

    public Map<SmsFilterMode, String> getModeMap() {
        Resources resources = getResources();
        HashMap<SmsFilterMode, String> modeMap = new HashMap<>(6);
        modeMap.put(SmsFilterMode.REGEX, resources.getString(R.string.filter_mode_regex));
        modeMap.put(SmsFilterMode.WILDCARD, resources.getString(R.string.filter_mode_wildcard));
        modeMap.put(SmsFilterMode.CONTAINS, resources.getString(R.string.filter_mode_contains));
        modeMap.put(SmsFilterMode.PREFIX, resources.getString(R.string.filter_mode_prefix));
        modeMap.put(SmsFilterMode.SUFFIX, resources.getString(R.string.filter_mode_suffix));
        modeMap.put(SmsFilterMode.EQUALS, resources.getString(R.string.filter_mode_equals));
        return modeMap;
    }

    public Map<CaseSensitivity, String> getCaseMap() {
        Resources resources = getResources();
        HashMap<CaseSensitivity, String> caseMap = new HashMap<>(2);
        caseMap.put(CaseSensitivity.INSENSITIVE, resources.getString(R.string.filter_case_insensitive));
        caseMap.put(CaseSensitivity.SENSITIVE, resources.getString(R.string.filter_case_sensitive));
        return caseMap;
    }
}
