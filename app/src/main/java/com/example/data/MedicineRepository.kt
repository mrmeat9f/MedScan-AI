package com.example.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(
    private val medicineDao: MedicineDao,
    private val pillboxDao: PillboxDao,
    private val intakeLogDao: IntakeLogDao
) {
    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()

    suspend fun getAllMedicinesDirect(): List<Medicine> {
        return medicineDao.getAllMedicinesDirect()
    }

    suspend fun insert(medicine: Medicine): Long {
        return medicineDao.insertMedicine(medicine)
    }

    suspend fun update(medicine: Medicine) {
        medicineDao.updateMedicine(medicine)
    }

    suspend fun delete(medicine: Medicine) {
        medicineDao.deleteMedicine(medicine)
    }

    suspend fun deleteById(id: Int) {
        medicineDao.deleteMedicineById(id)
    }
    
    suspend fun getById(id: Int): Medicine? {
        return medicineDao.getMedicineById(id)
    }

    // --- PILLBOX METHODS ---

    val allPillboxes: Flow<List<Pillbox>> = pillboxDao.getAllPillboxes()

    suspend fun getAllPillboxesDirect(): List<Pillbox> {
        return pillboxDao.getAllPillboxesDirect()
    }

    suspend fun insertPillbox(pillbox: Pillbox): Long {
        return pillboxDao.insertPillbox(pillbox)
    }

    suspend fun updatePillbox(pillbox: Pillbox) {
        pillboxDao.updatePillbox(pillbox)
    }

    suspend fun deletePillbox(pillbox: Pillbox) {
        pillboxDao.deletePillbox(pillbox)
    }

    val allPillboxEntries: Flow<List<PillboxEntry>> = pillboxDao.getAllPillboxEntries()

    suspend fun getAllPillboxEntriesDirect(): List<PillboxEntry> {
        return pillboxDao.getAllPillboxEntriesDirect()
    }

    suspend fun insertPillboxEntry(entry: PillboxEntry): Long {
        return pillboxDao.insertPillboxEntry(entry)
    }

    suspend fun updatePillboxEntry(entry: PillboxEntry) {
        pillboxDao.updatePillboxEntry(entry)
    }

    suspend fun deletePillboxEntry(entry: PillboxEntry) {
        pillboxDao.deletePillboxEntry(entry)
    }

    suspend fun getPillboxEntryById(id: Int): PillboxEntry? {
        return pillboxDao.getPillboxEntryById(id)
    }

    // --- INTAKE LOG METHODS ---
    val allIntakeLogs: Flow<List<IntakeLog>> = intakeLogDao.getAllIntakeLogs()

    suspend fun getAllIntakeLogsDirect(): List<IntakeLog> {
        return intakeLogDao.getAllIntakeLogsDirect()
    }

    suspend fun insertIntakeLog(log: IntakeLog): Long {
        return intakeLogDao.insertIntakeLog(log)
    }

    suspend fun deleteIntakeLog(log: IntakeLog) {
        intakeLogDao.deleteIntakeLog(log)
    }

    suspend fun deleteAllIntakeLogs() {
        intakeLogDao.deleteAllIntakeLogs()
    }
}
