CREATE OR REPLACE
PROCEDURE cleanupTestDatabaseProc
IS
  TYPE vtab IS TABLE OF VARCHAR2(30);
  l_enabled_constraints vtab := vtab();
  l_enabled_constraint_tabs vtab := vtab();
  i NUMBER := 0;
  j NUMBER := 0;
  BEGIN
    FOR cur IN (SELECT table_name,constraint_name FROM user_constraints WHERE status = 'ENABLED' AND constraint_type = 'R') LOOP
      i := i + 1;
      l_enabled_constraints.extend(1);
      l_enabled_constraint_tabs.extend(1);
      l_enabled_constraint_tabs(i) := cur.table_name;
      l_enabled_constraints(i) := cur.constraint_name;
    END LOOP;
    WHILE j < i LOOP
      j := j + 1;
      EXECUTE IMMEDIATE 'alter table ' || l_enabled_constraint_tabs(j) || ' disable constraint ' || l_enabled_constraints(j);
    END LOOP;
    FOR cur IN (SELECT table_name FROM user_tables) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'truncate table ' || cur.table_name;
      EXCEPTION
      WHEN OTHERS THEN
      DBMS_OUTPUT.put_line(cur.table_name || ' ' || SQLERRM);
    END;
    END LOOP;
    j := 0;
    WHILE j < i LOOP
      j := j + 1;
      EXECUTE IMMEDIATE 'alter table ' || l_enabled_constraint_tabs(j) || ' enable constraint ' || l_enabled_constraints(j);
    END LOOP;
    EXECUTE IMMEDIATE 'drop sequence hibernate_sequence';
    EXECUTE IMMEDIATE 'create sequence hibernate_sequence start with 1 increment by 1';
  END;
