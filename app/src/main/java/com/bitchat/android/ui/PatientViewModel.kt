package com.bitchat.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bitchat.android.model.PatientRecord
import com.bitchat.android.model.MedicalUpdate
import com.bitchat.android.model.PatientStatus
import com.bitchat.android.model.Priority
import com.bitchat.android.model.PatientHistoryEntry
import com.bitchat.android.storage.PatientStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * ViewModel for managing patient data and medical records
 * Handles patient operations while preserving existing chat functionality
 * Now includes persistent storage support
 */
class PatientViewModel(application: Application) : AndroidViewModel(application) {
    
    // Patient data
    private val _patients = MutableLiveData<List<PatientRecord>>(emptyList())
    val patients: LiveData<List<PatientRecord>> = _patients
    
    private val _medicalUpdates = MutableLiveData<Map<String, List<MedicalUpdate>>>(emptyMap())
    val medicalUpdates: LiveData<Map<String, List<MedicalUpdate>>> = _medicalUpdates
    
    // UI state
    private val _selectedPatient = MutableLiveData<PatientRecord?>(null)
    val selectedPatient: LiveData<PatientRecord?> = _selectedPatient
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Storage manager
    private val storageManager = PatientStorageManager.getInstance(application)
    
    // Initialize data from storage or samples if storage is empty
    init {
        loadDataFromStorage()
    }
    
    private fun loadDataFromStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            
            val storedPatients = storageManager.loadPatients()
            val storedUpdates = storageManager.loadMedicalUpdates()
            
            withContext(Dispatchers.Main) {
                if (storedPatients.isNotEmpty()) {
                    _patients.value = storedPatients
                    _medicalUpdates.value = storedUpdates
                } else {
                    // If no stored data exists, load sample data
                    loadSampleData()
                }
                _isLoading.value = false
            }
        }
    }
    
    private fun saveDataToStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            _patients.value?.let { storageManager.savePatients(it) }
            _medicalUpdates.value?.let { storageManager.saveMedicalUpdates(it) }
        }
    }
    
    private fun loadSampleData() {
        val samplePatients = listOf(
            PatientRecord(
                patientId = "P123456",
                name = "Ahmed Al-Rashid",
                age = 34,
                gender = "Male",
                bloodType = "O+",
                allergies = listOf("Penicillin"),
                currentMedications = listOf("Metformin"),
                medicalHistory = "Type 2 Diabetes",
                presentingComplaint = "Chest pain, shortness of breath",
                treatment = "Oxygen therapy, cardiac monitoring",
                status = PatientStatus.STABLE,
                priority = Priority.MEDIUM,
                location = "Ward A, Bed 3",
                authorFingerprint = "doc001",
                lastModified = Date()
            ),
            PatientRecord(
                patientId = "P789012",
                name = "Fatima Hassan",
                age = 28,
                gender = "Female",
                bloodType = "A-",
                allergies = emptyList(),
                currentMedications = emptyList(),
                medicalHistory = "Previous C-section",
                presentingComplaint = "Severe abdominal pain",
                treatment = "Pain management, IV fluids",
                status = PatientStatus.CRITICAL,
                priority = Priority.HIGH,
                location = "Emergency Room",
                authorFingerprint = "doc002",
                lastModified = Date()
            ),
            PatientRecord(
                patientId = "P345678",
                name = "Omar Khalil",
                age = 45,
                gender = "Male",
                bloodType = "B+",
                allergies = listOf("Aspirin"),
                currentMedications = listOf("Lisinopril", "Atorvastatin"),
                medicalHistory = "Hypertension, High cholesterol",
                presentingComplaint = "Routine check-up",
                treatment = "Medication review completed",
                status = PatientStatus.TREATED,
                priority = Priority.LOW,
                location = "Outpatient",
                authorFingerprint = "doc001",
                lastModified = Date()
            )
        )
        
        _patients.value = samplePatients
        
        // Sample medical updates
        val sampleUpdates = mapOf(
            "P123456" to listOf(
                MedicalUpdate(
                    patientId = "P123456",
                    updateType = com.bitchat.android.model.UpdateType.ASSESSMENT,
                    notes = "Patient responding well to treatment. Vitals stable.",
                    authorFingerprint = "doc001",
                    timestamp = Date(System.currentTimeMillis() - 3600000) // 1 hour ago
                )
            ),
            "P789012" to listOf(
                MedicalUpdate(
                    patientId = "P789012",
                    updateType = com.bitchat.android.model.UpdateType.STATUS_CHANGE,
                    notes = "Moved to critical care. Continuous monitoring required.",
                    authorFingerprint = "doc002",
                    timestamp = Date(System.currentTimeMillis() - 1800000) // 30 minutes ago
                )
            )
        )
        
        _medicalUpdates.value = sampleUpdates
        
        // Save sample data to storage
        saveDataToStorage()
    }
    
    fun selectPatient(patient: PatientRecord) {
        _selectedPatient.value = patient
    }
    
    fun clearSelectedPatient() {
        _selectedPatient.value = null
    }
    
    fun addPatient(patient: PatientRecord) {
        val currentPatients = _patients.value.orEmpty().toMutableList()
        currentPatients.add(patient)
        _patients.value = currentPatients
        saveDataToStorage()
    }
    
    fun updatePatient(updatedPatient: PatientRecord) {
        val currentPatients = _patients.value ?: emptyList()
        _patients.value = currentPatients.map { patient ->
            if (patient.id == updatedPatient.id) {
                updatedPatient.copy(
                    lastModified = Date(),
                    version = patient.version + 1
                )
            } else {
                patient
            }
        }
        saveDataToStorage()
    }
    
    /**
     * Adds a history entry (comment) to a patient record
     */
    fun addHistoryEntry(patientId: String, text: String, authorFingerprint: String = "") {
        val currentPatients = _patients.value ?: emptyList()
        val patient = currentPatients.find { it.patientId == patientId || it.id == patientId }
        
        patient?.let {
            val newEntry = PatientHistoryEntry(
                text = text,
                authorFingerprint = authorFingerprint,
                timestamp = Date()
            )
            
            val updatedPatient = it.copy(
                historyEntries = it.historyEntries + newEntry,
                lastModified = Date(),
                version = it.version + 1
            )
            
            updatePatient(updatedPatient)
            // Note: saveDataToStorage() is called inside updatePatient
        }
    }
    
    fun addMedicalUpdate(update: MedicalUpdate) {
        val currentUpdates = _medicalUpdates.value ?: emptyMap()
        val patientUpdates = currentUpdates[update.patientId] ?: emptyList()
        _medicalUpdates.value = currentUpdates + (update.patientId to (patientUpdates + update))
        saveDataToStorage()
    }
    
    fun getPatientById(patientId: String): PatientRecord? {
        return _patients.value?.find { it.patientId == patientId }
    }
    
    fun getMedicalUpdatesForPatient(patientId: String): List<MedicalUpdate> {
        return _medicalUpdates.value?.get(patientId) ?: emptyList()
    }
    
    // Statistics for dashboard
    fun getTotalPatientCount(): Int {
        return _patients.value?.size ?: 0
    }
    
    fun getCriticalPatientsCount(): Int {
        return _patients.value?.count { it.status == PatientStatus.CRITICAL } ?: 0
    }
    
    fun getPatientsByStatus(status: PatientStatus): List<PatientRecord> {
        return _patients.value?.filter { it.status == status } ?: emptyList()
    }
    
    fun getPatientsByPriority(priority: Priority): List<PatientRecord> {
        return _patients.value?.filter { it.priority == priority } ?: emptyList()
    }
    
    /**
     * Deletes a patient from the database
     * Also removes all associated medical updates
     */
    fun deletePatient(patientId: String) {
        // Remove patient from patients list
        val currentPatients = _patients.value ?: emptyList()
        _patients.value = currentPatients.filter { it.patientId != patientId && it.id != patientId }
        
        // Remove all medical updates for this patient
        val currentUpdates = _medicalUpdates.value ?: emptyMap()
        val updatedMap = currentUpdates.toMutableMap()
        updatedMap.remove(patientId)
        _medicalUpdates.value = updatedMap
        
        // Clear selected patient if it's the one being deleted
        if (_selectedPatient.value?.patientId == patientId || _selectedPatient.value?.id == patientId) {
            clearSelectedPatient()
        }
        
        saveDataToStorage()
    }
}
