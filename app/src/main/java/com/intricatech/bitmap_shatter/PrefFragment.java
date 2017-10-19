package com.intricatech.bitmap_shatter;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Bolgbolg on 16/10/2017.
 */

public class PrefFragment extends PreferenceFragment{

    private ListPreference minDepthPreference, maxDepthPreference;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        minDepthPreference = (ListPreference) findPreference(getString(R.string.pref_key_min_recursive_depth));
        minDepthPreference.setSummary(minDepthPreference.getValue());
        minDepthPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                minDepthPreference.setSummary(String.valueOf(newValue));
                return true;
            }
        });

        maxDepthPreference = (ListPreference) findPreference(getString(R.string.pref_key_max_recursive_depth));
        maxDepthPreference.setSummary(maxDepthPreference.getValue());
        maxDepthPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                maxDepthPreference.setSummary(String.valueOf(newValue));
                return true;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(R.color.preference_fragment_background));

        return view;

    }
}
