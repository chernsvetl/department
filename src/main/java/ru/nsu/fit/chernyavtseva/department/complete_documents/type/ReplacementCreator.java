package ru.nsu.fit.chernyavtseva.department.complete_documents.type;

import com.github.petrovich4j.Case;
import com.github.petrovich4j.Gender;
import com.github.petrovich4j.NameType;
import com.github.petrovich4j.Petrovich;

import java.util.Map;
import java.util.Objects;

/**
 * Извлекает значение из строки студента (результат SQL-запроса {@link ru.nsu.fit.chernyavtseva.department.query_model.QueryExecutor})
 * и при необходимости приводит ФИО к нужному падежу (библиотека Petrovich).
 */
public sealed interface ReplacementCreator {

    static ReplacementCreator simple(String solutionVarName) {
        return new Simple(solutionVarName);
    }

    static ReplacementCreator fullName(String solutionVarName, Case wordCase) {
        return new FullNameReplacement(solutionVarName, wordCase);
    }

    static ReplacementCreator gender(String solutionVarName) {
        return new GenderStudentReplacement(solutionVarName);

    }

    static ReplacementCreator genderFio(String solutionVarName) {
        return new GenderStudentImReplacement(solutionVarName);

    }

    static ReplacementCreator genderForm(String solutionVarName) {
        return new GenderFormStudentReplacement(solutionVarName);

    }

    static ReplacementCreator genderFormTwor(String solutionVarName) {
        return new GenderStudentTworReplacement(solutionVarName);

    }

    static ReplacementCreator genderFormImStud(String solutionVarName) {
        return new GenderStudentImStudReplacement(solutionVarName);

    }

    static ReplacementCreator genderFormDatStud(String solutionVarName) {
        return new GenderDatFormStudentReplacement(solutionVarName);

    }

    String replacement(Map<String, String> student);
}

final class Simple implements ReplacementCreator {

    private final String varName;

    Simple(String varName) {
        this.varName = varName;
    }


    @Override
    public String replacement(Map<String, String> student) {
        return student.get(varName);
    }

}

final class FullNameReplacement implements ReplacementCreator {

    private final String solutionVarName;
    private final Case wordCase;

    FullNameReplacement(String solutionVarName, Case wordCase) {
        this.solutionVarName = solutionVarName;
        this.wordCase = wordCase;
    }

    @Override
    public String replacement(Map<String, String> student) {
        String fullName = student.get(solutionVarName);
        if (fullName == null) {
            return null;
        }
        String[] nameChunks = fullName.split(" ");
        if (nameChunks.length == 4) {
            return nameChunks[0] + 'а' + " " + nameChunks[1] + 'а' + " " + nameChunks[2] + " " + nameChunks[3];
        }
        else if (nameChunks.length == 2) {
            return nameChunks[0] + 'а' + " " + nameChunks[1] + 'а';
        }
        else if (nameChunks.length == 3) {
            Petrovich petrovich = new Petrovich();
            Gender gender = petrovich.gender(nameChunks[2], Gender.Both);
            String lastName = petrovich.say(nameChunks[0], NameType.LastName, gender, wordCase);
            String firstName = petrovich.say(nameChunks[1], NameType.FirstName, gender, wordCase);
            String patronymicName = petrovich.say(nameChunks[2], NameType.PatronymicName, gender, wordCase);
    // the best way because library does not support that case
            if (Objects.equals(nameChunks[0], "Кривошея")) {

                return String.format(lastName.replaceFirst("я","и") + " " + firstName + " " +patronymicName);
            } else
            return String.format(lastName + " " + firstName + " " + patronymicName);
        } else {
            throw new IllegalArgumentException("No full name in variable " + solutionVarName + "; value: " + fullName);
        }
    }
}

final class GenderStudentReplacement implements ReplacementCreator {

    private final String solutionVarName;

    GenderStudentReplacement(String solutionVarName) {
        this.solutionVarName = solutionVarName;
    }

    @Override
    public String replacement(Map<String, String> student) {
        String fullName = student.get(solutionVarName);
        if (fullName == null) {
            return null;
        }
        String[] nameChunks = fullName.split(" ");
        if (nameChunks.length == 4) {
            return "Обучающегося";
        }
        if (nameChunks.length == 2) {
            return "Обучающегося";
        }
        else if (nameChunks.length == 3) {
            Petrovich petrovich = new Petrovich();
            Gender gender = petrovich.gender(nameChunks[2], Gender.Both);
            // the best way because library does not support that case
            switch (gender) {
                case Female -> {
                    return "Обучающейся";
                }
                default -> {
                    return "Обучающегося";
                }
            }
        } else {
            throw new IllegalArgumentException("No full name in variable " + solutionVarName + "; value: " + fullName);
        }
    }
}


final class GenderFormStudentReplacement implements ReplacementCreator {

    private final String solutionVarName;

    GenderFormStudentReplacement(String solutionVarName) {
        this.solutionVarName = solutionVarName;
    }

    @Override
    public String replacement(Map<String, String> student) {
        String fullName = student.get(solutionVarName);
        if (fullName == null) {
            return null;
        }
        String[] nameChunks = fullName.split(" ");
        if (nameChunks.length == 4) {
            return "студенту";
        }
        if (nameChunks.length == 2) {
            return "студенту";
        }
        else if (nameChunks.length == 3) {
            Petrovich petrovich = new Petrovich();
            Gender gender = petrovich.gender(nameChunks[2], Gender.Both);
            // the best way because library does not support that case
            switch (gender) {
                case Female -> {
                    return "студентке";
                }
                default -> {
                    return "студенту";
                }
            }
        } else {
            throw new IllegalArgumentException("No full name in variable " + solutionVarName + "; value: " + fullName);
        }
    }
}

final class GenderStudentImReplacement implements ReplacementCreator {

    private final String solutionVarName;

    GenderStudentImReplacement(String solutionVarName) {
        this.solutionVarName = solutionVarName;
    }

    @Override
    public String replacement(Map<String, String> student) {
        String fullName = student.get(solutionVarName);
        if (fullName == null) {
            return null;
        }
        String[] nameChunks = fullName.split(" ");
        if (nameChunks.length == 4) {
            return "Обучающийся";
        }
        if (nameChunks.length == 2) {
            return "Обучающийся";
        }
        else if (nameChunks.length == 3) {
            Petrovich petrovich = new Petrovich();
            Gender gender = petrovich.gender(nameChunks[2], Gender.Both);
            // the best way because library does not support that case
            switch (gender) {
                case Female -> {
                    return "Обучающаяся";
                }
                default -> {
                    return "Обучающийся";
                }
            }
        } else {
            throw new IllegalArgumentException("No full name in variable " + solutionVarName + "; value: " + fullName);
        }
    }
}


final class GenderStudentTworReplacement implements ReplacementCreator {

    private final String solutionVarName;

    GenderStudentTworReplacement(String solutionVarName) {
        this.solutionVarName = solutionVarName;
    }

    @Override
    public String replacement(Map<String, String> student) {
        String fullName = student.get(solutionVarName);
        if (fullName == null) {
            return null;
        }
        String[] nameChunks = fullName.split(" ");
        if (nameChunks.length == 4) {
            return "студентом";
        }
        if (nameChunks.length == 2) {
            return "студентом";
        }
        else if (nameChunks.length == 3) {
            Petrovich petrovich = new Petrovich();
            Gender gender = petrovich.gender(nameChunks[2], Gender.Both);
            switch (gender) {
                case Female -> {
                    return "студенткой";
                }
                default -> {
                    return "студентом";
                }
            }
        } else {
            throw new IllegalArgumentException("No full name in variable " + solutionVarName + "; value: " + fullName);
        }
    }
}


final class GenderStudentImStudReplacement implements ReplacementCreator {

    private final String solutionVarName;

    GenderStudentImStudReplacement(String solutionVarName) {
        this.solutionVarName = solutionVarName;
    }

    @Override
    public String replacement(Map<String, String> student) {
        String fullName = student.get(solutionVarName);
        if (fullName == null) {
            return null;
        }
        String[] nameChunks = fullName.split(" ");
        if (nameChunks.length == 4) {
            return "Студент";
        }
        if (nameChunks.length == 2) {
            return "Студент";
        }
        else if (nameChunks.length == 3) {
            Petrovich petrovich = new Petrovich();
            Gender gender = petrovich.gender(nameChunks[2], Gender.Both);
            switch (gender) {
                case Female -> {
                    return "Студентка";
                }
                default -> {
                    return "Студент";
                }
            }
        } else {
            throw new IllegalArgumentException("No full name in variable " + solutionVarName + "; value: " + fullName);
        }
    }
}



final class GenderDatFormStudentReplacement implements ReplacementCreator {

    private final String solutionVarName;

    GenderDatFormStudentReplacement(String solutionVarName) {
        this.solutionVarName = solutionVarName;
    }

    @Override
    public String replacement(Map<String, String> student) {
        String fullName = student.get(solutionVarName);
        if (fullName == null) {
            return null;
        }
        String[] nameChunks = fullName.split(" ");
        if (nameChunks.length == 4) {
            return "Студенту";
        }
        if (nameChunks.length == 2) {
            return "Студенту";
        }
        else if (nameChunks.length == 3) {
            Petrovich petrovich = new Petrovich();
            Gender gender = petrovich.gender(nameChunks[2], Gender.Both);
            switch (gender) {
                case Female -> {
                    return "Студентке";
                }
                default -> {
                    return "Студенту";
                }
            }
        } else {
            throw new IllegalArgumentException("No full name in variable " + solutionVarName + "; value: " + fullName);
        }
    }
}

