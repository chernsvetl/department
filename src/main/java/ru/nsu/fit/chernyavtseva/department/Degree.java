package ru.nsu.fit.chernyavtseva.department;

import ru.nsu.fit.chernyavtseva.department.complete_documents.type.core.DocumentTemplate;
import ru.nsu.fit.chernyavtseva.department.complete_documents.type.course2.mda.*;
import ru.nsu.fit.chernyavtseva.department.complete_documents.type.course2.trps.*;
import ru.nsu.fit.chernyavtseva.department.complete_documents.type.course4.knis.*;
import ru.nsu.fit.chernyavtseva.department.complete_documents.type.course4.piikn.*;

/**
 * Выпуск (направление + курс + профиль), по которому выбираются студенты из базы данных
 * и документы, которые для них заполняются.
 *
 * <p>Каждая реализация описывает одну "группу студентов":
 * <ul>
 *     <li>{@link #name()}  — ступень обучения ("Бакалавриат"/"Магистратура"), поле degree в БД;</li>
 *     <li>{@link #course()} — курс, поле course в БД;</li>
 *     <li>{@link #profile()} — направленность (профиль), поле profile в БД;</li>
 *     <li>{@link #dir()} — подпапка с шаблонами .docx и папка, куда складываются готовые документы;</li>
 *     <li>{@link #toGenerate()} — какие шаблоны заполнять (см. {@link DocumentTemplate}).</li>
 * </ul>
 */
sealed public interface Degree {

    Degree[] DEGREES = new Degree[]{new BachelorPikn(), new BachelorKnis(), new MasterMDA(), new MasterTRPS()};

    static Degree[] all() {
        return DEGREES;
    }

    String name();

    int course();

    String dir();

    String profile();

    DocumentTemplate[] toGenerate();
}

/**
 * Бакалавриат 4 курс, ПИиКН, 8 семестр — полный комплект документов по ВКР и преддипломной практике.
 */
final class BachelorPikn implements Degree {

    /**
     * Degree name, used inside query
     */
    @Override
    public String name() {
        return "Бакалавриат";
    }

    @Override
    public int course() {
        return 4;
    }

    /**
     * Subdirectory where to search for tempates / where to dump generated docs
     */
    @Override
    public String dir() {
        return "bachelors/4th_course/piikn";
    }

    @Override
    public String profile() {
        return "Программная инженерия и компьютерные науки";
    }

    /**
     * Which templates should be generated
     */
    @Override
    public DocumentTemplate[] toGenerate() {
        return new DocumentTemplate[]{new BachelorSupervisorFeedback(),
                 new BachelorIndividualTask(), new BachelorPracticeReport(),
                new BachelorPracticeFeedback(), new BachelorSupervisorFeedback2(),
                new BachelorApplicationForPractice(), new BachelorAnnotationPikn(),
                new BachelorCalenderPikn(), new BachelorListLiteraturePikn(),
                new BachelorLastPagePikn(), new BachelorNormPikn(),
                new BachelorTaskPikn(), new BachelorTaskPikn2(),
                new BachelorTitulPikn(), new BachelorCalenderPikn2(),
                new BachelorPublicPikn(), new BachelorTaskPiknWithCorrections(),
                new BachelorTaskPiknWithCorrections2()
        };
    }
}

/**
 * Бакалавриат 4 курс, КНиС — документы по ВКР (список шаблонов включается по мере необходимости).
 */
final class BachelorKnis implements Degree {

    /**
     * Degree name, used inside query
     */
    @Override
    public String name() {
        return "Бакалавриат";
    }

    @Override
    public int course() {
        return 4;
    }

    /**
     * Subdirectory where to search for tempates / where to dump generated docs
     */
    @Override
    public String dir() {
        return "bachelors/4th_course/knis";
    }

    @Override
    public String profile() {
        return "Компьютерные науки и системотехника";
    }

    /**
     * Which templates should be generated
     */
    @Override
    public DocumentTemplate[] toGenerate() {


        return new DocumentTemplate[]{
                /*
                new BachelorSupervisorFeedbackKnis(),
                new BachelorIndividualTaskKnis(),
                new BachelorPracticeReportKnis(),
                new BachelorPracticeFeedbackKnis(),
                new BachelorSupervisorFeedback2Knis(),
                new BachelorApplicationForPracticeKnis(),
                new BachelorAnnotationKnis(),
                new BachelorCalenderKnis(), new BachelorListLiteratureKnis(),
                new BachelorLastPageKnis(), new BachelorNormKnis(),
                new BachelorTaskKnis(), new BachelorTaskKnis2(),
                new BachelorTitulKnis(), new BachelorCalenderKnis2(), new BachelorPublicKnis()
        */
        };
    }
}

/**
 * Магистратура 2 курс, МДА, 4 семестр — документы по ВКР и практике
 * (список шаблонов включается по мере необходимости).
 */
final class MasterMDA implements Degree {

    /**
     * Degree name, used inside query
     */
    @Override
    public String name() {
        return "Магистратура";
    }

    @Override
    public int course() {
        return 2;
    }

    /**
     * Subdirectory where to search for tempates / where to dump generated docs
     */
    @Override
    public String dir() {
        return "masters/2nd_course/mda";
    }

    @Override
    public String profile() {
        return "Компьютерное моделирование и анализ данных";
    }

    /**
     * Which templates should be generated
     */
    @Override
    public DocumentTemplate[] toGenerate() {


        return new DocumentTemplate[]{
                /*
                new MasterSupervisorFeedbackMDA(), new MasterSupervisorFeedbackMDA2(),
                new MasterReviewerFeedbackMDA(), new MasterIndividualTaskMDA(),
                new MasterPracticeReportMDA(), new MasterPracticeFeedbackMDA(),
                new MasterApplicationForPracticeMDA(),
                new MasterAnnotationMda(), new MasterCalenderMda2(),
                new MasterCalenderMda(), new MasterListLiteratureMda(),
                new MasterLastPageMda(), new MasterNormMda(),
                new MasterTaskMda(), new MasterTaskMda2(),
                new MasterTitulMda(),  new MasterPublicMad()
                 */
        };
    }
}

/**
 * Магистратура 2 курс, ТРПС, 4 семестр — документы по ВКР и практике
 * (список шаблонов включается по мере необходимости).
 */
final class MasterTRPS implements Degree {

    @Override
    public String name() {
        return "Магистратура";
    }

    @Override
    public int course() {
        return 2;
    }

    @Override
    public String dir() {
        return "masters/2nd_course/trps";
    }

    @Override
    public String profile() {
        return "Технология разработки программных систем";
    }

    @Override
    public DocumentTemplate[] toGenerate() {
        return new DocumentTemplate[]{
                /*
                new MasterSupervisorFeedbackTRPS(),
                new MasterReviewerFeedbackTRPS(), new MasterIndividualTaskTRPS(),
                new MasterPracticeReportTRPS(), new MasterPracticeFeedbackTRPS(),
                new MasterSupervisorFeedbackTRPS2(), new MasterApplicationForPracticeTRPS(),
                new MasterAnnotationTrps(), new MasterCalenderTrps(), new MasterListLiteratureTrps(),
                new MasterLastPageTrps(), new MasterNormTrps(),
                new MasterTaskTrps(), new MasterTaskTrps2(),
                new MasterTitulTrps(), new MasterCalenderTrps2(),  new MasterPublicTrps()

   */
        };
    }
}
