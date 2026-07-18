package com.vi5hnu.calculator.data.repository

import com.vi5hnu.calculator.data.local.HistoryDao
import com.vi5hnu.calculator.data.local.HistoryEntity
import com.vi5hnu.calculator.domain.model.HistoryEntry
import com.vi5hnu.calculator.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val dao: HistoryDao,
) : HistoryRepository {

    override fun observeHistory(): Flow<List<HistoryEntry>> =
        dao.observeAll().map { rows -> rows.map(HistoryEntity::toDomain) }

    override suspend fun add(expression: String, result: Double): Long {
        val id = dao.insert(
            HistoryEntity(
                expression = expression,
                result = result,
                createdAt = System.currentTimeMillis(),
            ),
        )
        dao.trimTo(MAX_ENTRIES)
        return id
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.clear()

    private companion object {
        const val MAX_ENTRIES = 200
    }
}

private fun HistoryEntity.toDomain() = HistoryEntry(
    id = id,
    expression = expression,
    result = result,
    pinned = pinned,
    createdAt = createdAt,
)
