package com.mathgame;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ScoreDao_Impl implements ScoreDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ScoreEntry> __insertionAdapterOfScoreEntry;

  private final SharedSQLiteStatement __preparedStmtOfPruneScores;

  public ScoreDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfScoreEntry = new EntityInsertionAdapter<ScoreEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `scores` (`id`,`score`,`correctCount`,`totalQuestions`,`accuracy`,`operation`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ScoreEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getScore());
        statement.bindLong(3, entity.getCorrectCount());
        statement.bindLong(4, entity.getTotalQuestions());
        statement.bindDouble(5, entity.getAccuracy());
        statement.bindString(6, entity.getOperation());
        statement.bindLong(7, entity.getTimestamp());
      }
    };
    this.__preparedStmtOfPruneScores = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM scores WHERE score < (SELECT MIN(score) FROM (SELECT * FROM scores ORDER BY score DESC LIMIT 100))";
        return _query;
      }
    };
  }

  @Override
  public void insert(final ScoreEntry entry) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfScoreEntry.insert(entry);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public Object pruneScores(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneScores.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfPruneScores.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getTopScores(final int limit,
      final Continuation<? super List<ScoreEntry>> $completion) {
    final String _sql = "SELECT * FROM scores ORDER BY score DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ScoreEntry>>() {
      @Override
      @NonNull
      public List<ScoreEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final int _cursorIndexOfCorrectCount = CursorUtil.getColumnIndexOrThrow(_cursor, "correctCount");
          final int _cursorIndexOfTotalQuestions = CursorUtil.getColumnIndexOrThrow(_cursor, "totalQuestions");
          final int _cursorIndexOfAccuracy = CursorUtil.getColumnIndexOrThrow(_cursor, "accuracy");
          final int _cursorIndexOfOperation = CursorUtil.getColumnIndexOrThrow(_cursor, "operation");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<ScoreEntry> _result = new ArrayList<ScoreEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ScoreEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpScore;
            _tmpScore = _cursor.getInt(_cursorIndexOfScore);
            final int _tmpCorrectCount;
            _tmpCorrectCount = _cursor.getInt(_cursorIndexOfCorrectCount);
            final int _tmpTotalQuestions;
            _tmpTotalQuestions = _cursor.getInt(_cursorIndexOfTotalQuestions);
            final float _tmpAccuracy;
            _tmpAccuracy = _cursor.getFloat(_cursorIndexOfAccuracy);
            final String _tmpOperation;
            _tmpOperation = _cursor.getString(_cursorIndexOfOperation);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new ScoreEntry(_tmpId,_tmpScore,_tmpCorrectCount,_tmpTotalQuestions,_tmpAccuracy,_tmpOperation,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
