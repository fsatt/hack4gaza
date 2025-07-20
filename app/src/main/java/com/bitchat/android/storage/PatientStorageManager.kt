package com.bitchat.android.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bitchat.android.model.PatientRecord
import com.bitchat.android.model.MedicalUpdate
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Manages persistent storage of patient records and related data
 * Uses SharedPreferences with JSON serialization
 */
class PatientStorageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PatientStorageManager"
        private const val PREF_NAME = "patient_data"
        private const val KEY_PATIENTS = "patients"
        private const val KEY_MEDICAL_UPDATES = "medical_updates"
        
        @Volatile
        private var INSTANCE: PatientStorageManager? = null
        
        fun getInstance(context: Context): PatientStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PatientStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    /**
     * Save all patient records to persistent storage
     */
    fun savePatients(patients: List<PatientRecord>) {
        try {
            val patientsJson = gson.toJson(patients)
            prefs.edit().putString(KEY_PATIENTS, patientsJson).apply()
            Log.d(TAG, "Saved ${patients.size} patients to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving patients", e)
        }
    }
    
    /**
     * Load all patient records from persistent storage
     */
    fun loadPatients(): List<PatientRecord> {
        try {
            val patientsJson = prefs.getString(KEY_PATIENTS, null) ?: return emptyList()
            val type = object : TypeToken<List<PatientRecord>>() {}.type
            val patients: List<PatientRecord> = gson.fromJson(patientsJson, type)
            Log.d(TAG, "Loaded ${patients.size} patients from storage")
            return patients
        } catch (e: Exception) {
            Log.e(TAG, "Error loading patients", e)
            return emptyList()
        }
    }
    
    /**
     * Save all medical updates to persistent storage
     */
    fun saveMedicalUpdates(updates: Map<String, List<MedicalUpdate>>) {
        try {
            val updatesJson = gson.toJson(updates)
            prefs.edit().putString(KEY_MEDICAL_UPDATES, updatesJson).apply()
            Log.d(TAG, "Saved medical updates for ${updates.size} patients to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving medical updates", e)
        }
    }
    
    /**
     * Load all medical updates from persistent storage
     */
    fun loadMedicalUpdates(): Map<String, List<MedicalUpdate>> {
        try {
            val updatesJson = prefs.getString(KEY_MEDICAL_UPDATES, null) ?: return emptyMap()
            val type = object : TypeToken<Map<String, List<MedicalUpdate>>>() {}.type
            val updates: Map<String, List<MedicalUpdate>> = gson.fromJson(updatesJson, type)
            Log.d(TAG, "Loaded medical updates for ${updates.size} patients from storage")
            return updates
        } catch (e: Exception) {
            Log.e(TAG, "Error loading medical updates", e)
            return emptyMap()
        }
    }
    
    /**
     * Clear all patient data from storage
     */
    fun clearAllData() {
        prefs.edit()
            .remove(KEY_PATIENTS)
            .remove(KEY_MEDICAL_UPDATES)
            .apply()
        Log.d(TAG, "Cleared all patient data from storage")
    }
}
