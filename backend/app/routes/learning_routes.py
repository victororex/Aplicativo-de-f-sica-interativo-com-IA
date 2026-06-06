import json
import sqlite3

from fastapi import APIRouter, Depends, HTTPException

from app.database import get_db
from app.schemas.learning_schema import (
    AvatarItemResponse,
    CampaignSubmission,
    CampaignNodeResponse,
    DailyQuestionResponse,
    DailyChallengeStatusResponse,
    QuizResultResponse,
    QuizSubmission,
)
from app.security import get_current_user, get_optional_user

router = APIRouter()


@router.get("/daily-challenge", response_model=list[DailyQuestionResponse])
def daily_challenge(db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute(
        """
        SELECT q.*, s.name AS subject_name
        FROM daily_questions q
        JOIN subjects s ON s.id = q.subject_id
        ORDER BY q.sort_order ASC
        """
    ).fetchall()
    return [
        DailyQuestionResponse(
            id=row["id"],
            question=row["question"],
            options=json.loads(row["options_json"]),
            correct_index=row["correct_index"],
            explanation=row["explanation"],
            subject_id=row["subject_id"],
            subject_name=row["subject_name"],
        )
        for row in rows
    ]


@router.get("/daily-challenge/status", response_model=DailyChallengeStatusResponse)
def daily_challenge_status(
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    attempt = _today_attempt(db, current_user["id"])
    if attempt is None:
        return DailyChallengeStatusResponse(completed_today=False)

    accuracy = round((attempt["score"] / attempt["total"]) * 100) if attempt["total"] else 0
    return DailyChallengeStatusResponse(
        completed_today=True,
        score=attempt["score"],
        total=attempt["total"],
        accuracy_rate=accuracy,
        completed_at=attempt["created_at"],
    )


@router.post("/daily-challenge/submit", response_model=QuizResultResponse)
def submit_daily_challenge(
    request: QuizSubmission,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    if request.total <= 0 or request.score < 0 or request.score > request.total:
        raise HTTPException(status_code=422, detail="Pontuacao invalida.")
    if _today_attempt(db, current_user["id"]) is not None:
        raise HTTPException(status_code=409, detail="O desafio diario de hoje ja foi concluido.")

    db.execute(
        "INSERT INTO quiz_attempts (user_id, score, total) VALUES (?, ?, ?)",
        (current_user["id"], request.score, request.total),
    )
    db.execute(
        """
        UPDATE study_progress
        SET answered_exercises = answered_exercises + ?,
            correct_answers = correct_answers + ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = ?
        """,
        (request.total, request.score, current_user["id"]),
    )
    db.commit()
    return QuizResultResponse(
        score=request.score,
        total=request.total,
        accuracy_rate=round((request.score / request.total) * 100),
    )


def _today_attempt(db: sqlite3.Connection, user_id: int) -> sqlite3.Row | None:
    return db.execute(
        """
        SELECT score, total, created_at
        FROM quiz_attempts
        WHERE user_id = ?
          AND date(created_at, 'localtime') = date('now', 'localtime')
        ORDER BY created_at DESC
        LIMIT 1
        """,
        (user_id,),
    ).fetchone()


@router.get("/campaign", response_model=list[CampaignNodeResponse])
def campaign(
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row | None = Depends(get_optional_user),
):
    user_id = current_user["id"] if current_user else None
    rows = db.execute(
        """
        SELECT c.*, s.name AS subject_name
        FROM campaign_nodes c
        JOIN subjects s ON s.id = c.subject_id
        ORDER BY c.sort_order ASC
        """
    ).fetchall()

    result = []
    previous_completed = True
    for row in rows:
        progress = _campaign_progress(db, row["id"], user_id)
        unlocked = previous_completed or progress > 0
        result.append(
            CampaignNodeResponse(
                id=row["id"],
                title=row["title"],
                description=row["description"],
                subject_id=row["subject_id"],
                subject_name=row["subject_name"],
                progress=progress,
                is_unlocked=unlocked,
                **_campaign_mission(row["subject_id"], row["subject_name"]),
            )
        )
        previous_completed = progress >= 1.0
    return result


@router.post("/campaign/{node_id}/submit", response_model=QuizResultResponse)
def submit_campaign_stage(
    node_id: str,
    request: CampaignSubmission,
    db: sqlite3.Connection = Depends(get_db),
    current_user: sqlite3.Row = Depends(get_current_user),
):
    if request.total <= 0 or request.score < 0 or request.score > request.total:
        raise HTTPException(status_code=422, detail="Pontuacao invalida.")
    node = db.execute("SELECT id FROM campaign_nodes WHERE id = ?", (node_id,)).fetchone()
    if node is None:
        raise HTTPException(status_code=404, detail="Fase nao encontrada.")

    db.execute(
        """
        INSERT INTO campaign_progress (user_id, node_id, score, total)
        VALUES (?, ?, ?, ?)
        ON CONFLICT(user_id, node_id) DO UPDATE SET
            score = excluded.score,
            total = excluded.total,
            completed_at = CURRENT_TIMESTAMP
        """,
        (current_user["id"], node_id, request.score, request.total),
    )
    db.execute(
        """
        UPDATE study_progress
        SET answered_exercises = answered_exercises + ?,
            correct_answers = correct_answers + ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE user_id = ?
        """,
        (request.total, request.score, current_user["id"]),
    )
    db.commit()
    return QuizResultResponse(
        score=request.score,
        total=request.total,
        accuracy_rate=round((request.score / request.total) * 100),
    )


@router.get("/avatar/items", response_model=list[AvatarItemResponse])
def avatar_items(db: sqlite3.Connection = Depends(get_db)):
    rows = db.execute(
        """
        SELECT id, category, name
        FROM avatar_items
        ORDER BY category ASC, sort_order ASC
        """
    ).fetchall()
    return [AvatarItemResponse(**dict(row)) for row in rows]


def _subject_progress(db: sqlite3.Connection, subject_id: str, user_id: int | None) -> float:
    total = db.execute("SELECT COUNT(*) AS total FROM lessons WHERE subject_id = ?", (subject_id,)).fetchone()["total"]
    if not total or user_id is None:
        return 0.0
    completed = db.execute(
        """
        SELECT COUNT(*) AS completed
        FROM lesson_progress lp
        JOIN lessons l ON l.id = lp.lesson_id
        WHERE lp.user_id = ? AND l.subject_id = ?
        """,
        (user_id, subject_id),
    ).fetchone()["completed"]
    return completed / total if total else 0.0


def _campaign_progress(db: sqlite3.Connection, node_id: str, user_id: int | None) -> float:
    if user_id is None:
        return 0.0
    progress = db.execute(
        """
        SELECT score, total
        FROM campaign_progress
        WHERE user_id = ? AND node_id = ?
        """,
        (user_id, node_id),
    ).fetchone()
    if progress is None or not progress["total"]:
        return 0.0
    return min(progress["score"] / progress["total"], 1.0)


def _campaign_mission(subject_id: str, subject_name: str) -> dict:
    missions = {
        "mecanica": {
            "stage_label": "Fase do movimento",
            "visual_type": "motion",
            "exercises": [
                {
                    "id": "mecanica-1",
                    "question": "Dois blocos recebem o mesmo empurrao. Qual muda de movimento com mais facilidade?",
                    "options": ["O bloco mais leve", "O bloco mais pesado", "Os dois iguais"],
                    "correct_index": 0,
                    "explanation": "Com o mesmo empurrao, o corpo mais leve acelera mais.",
                    "visual_type": "motion",
                },
                {
                    "id": "mecanica-2",
                    "question": "Se o bloco continua parado, o que provavelmente esta acontecendo?",
                    "options": ["Outra forca esta equilibrando", "Ele nao tem peso", "A energia sumiu"],
                    "correct_index": 0,
                    "explanation": "Quando as forcas se equilibram, o movimento nao muda.",
                    "visual_type": "motion",
                },
                {
                    "id": "mecanica-3",
                    "question": "Na linha do movimento, o que uma subida mais inclinada mostra?",
                    "options": ["Maior rapidez", "Menor massa", "Mais calor"],
                    "correct_index": 0,
                    "explanation": "Quanto mais inclinada a linha, mais rapido o movimento cresce.",
                    "visual_type": "motion",
                },
            ],
        },
        "termologia": {
            "stage_label": "Fase do calor",
            "visual_type": "heat",
            "exercises": [
                {
                    "id": "termologia-1",
                    "question": "Para onde o calor vai quando um corpo quente encosta em um frio?",
                    "options": ["Do quente para o frio", "Do frio para o quente", "Ele fica parado"],
                    "correct_index": 0,
                    "explanation": "O calor caminha naturalmente do mais quente para o mais frio.",
                    "visual_type": "heat",
                },
                {
                    "id": "termologia-2",
                    "question": "Quando a linha de temperatura fica reta durante o aquecimento, o que pode estar acontecendo?",
                    "options": ["Mudanca de estado", "A massa desaparece", "O tempo parou"],
                    "correct_index": 0,
                    "explanation": "A energia esta sendo usada para mudar o estado, nao para aumentar a temperatura.",
                    "visual_type": "heat",
                },
                {
                    "id": "termologia-3",
                    "question": "Qual objeto perde calor primeiro ao encostar em algo frio?",
                    "options": ["O mais quente", "O mais frio", "Os dois ganham calor"],
                    "correct_index": 0,
                    "explanation": "O objeto mais quente entrega energia para o mais frio.",
                    "visual_type": "heat",
                },
            ],
        },
        "ondulatoria": {
            "stage_label": "Fase das ondas",
            "visual_type": "wave",
            "exercises": [
                {
                    "id": "ondulatoria-1",
                    "question": "Qual onda representa um som mais agudo?",
                    "options": ["A onda mais apertada", "A onda mais espalhada", "A linha reta"],
                    "correct_index": 0,
                    "explanation": "Som mais agudo tem mais repeticoes em menos tempo.",
                    "visual_type": "wave",
                },
                {
                    "id": "ondulatoria-2",
                    "question": "O que aumenta quando a crista da onda fica mais alta?",
                    "options": ["A intensidade", "A massa", "A temperatura"],
                    "correct_index": 0,
                    "explanation": "Cristas mais altas indicam uma onda mais intensa.",
                    "visual_type": "wave",
                },
                {
                    "id": "ondulatoria-3",
                    "question": "A distancia entre duas cristas mostra o que?",
                    "options": ["O tamanho da onda", "O peso da onda", "A cor da onda"],
                    "correct_index": 0,
                    "explanation": "Essa distancia mostra o comprimento da onda.",
                    "visual_type": "wave",
                },
            ],
        },
        "optica": {
            "stage_label": "Fase da luz",
            "visual_type": "light",
            "exercises": [
                {
                    "id": "optica-1",
                    "question": "O que acontece quando os caminhos da luz se encontram?",
                    "options": ["A imagem aparece", "A luz some", "O objeto fica mais pesado"],
                    "correct_index": 0,
                    "explanation": "A imagem se forma onde os raios de luz se encontram.",
                    "visual_type": "light",
                },
                {
                    "id": "optica-2",
                    "question": "Quando a luz bate no espelho, o que ela faz?",
                    "options": ["Volta em outro caminho", "Vira calor sempre", "Perde toda a energia"],
                    "correct_index": 0,
                    "explanation": "No espelho, a luz reflete e segue outro caminho.",
                    "visual_type": "light",
                },
                {
                    "id": "optica-3",
                    "question": "Uma lente pode ajudar a imagem a aparecer porque ela:",
                    "options": ["Muda o caminho da luz", "Apaga os raios", "Aumenta o peso do objeto"],
                    "correct_index": 0,
                    "explanation": "A lente desvia os caminhos da luz para formar a imagem.",
                    "visual_type": "light",
                },
            ],
        },
        "eletromagnetismo": {
            "stage_label": "Fase da energia",
            "visual_type": "circuit",
            "exercises": [
                {
                    "id": "eletro-1",
                    "question": "Quando a lampada acende em um circuito simples?",
                    "options": ["Quando o caminho esta fechado", "Quando o fio esta cortado", "Quando nao ha energia"],
                    "correct_index": 0,
                    "explanation": "A energia precisa de um caminho completo para circular.",
                    "visual_type": "circuit",
                },
                {
                    "id": "eletro-2",
                    "question": "Se um fio e cortado, o que acontece com a lampada?",
                    "options": ["Apaga", "Brilha mais", "Vira ima"],
                    "correct_index": 0,
                    "explanation": "Sem caminho completo, a energia nao circula.",
                    "visual_type": "circuit",
                },
                {
                    "id": "eletro-3",
                    "question": "A bateria no circuito serve para:",
                    "options": ["Empurrar a energia pelo caminho", "Esfriar a lampada", "Cortar o fio"],
                    "correct_index": 0,
                    "explanation": "A bateria cria a diferenca que faz a energia circular.",
                    "visual_type": "circuit",
                },
            ],
        },
        "fisica-moderna": {
            "stage_label": "Fase do invisivel",
            "visual_type": "atom",
            "exercises": [
                {
                    "id": "moderna-1",
                    "question": "O que fica no centro de um atomo?",
                    "options": ["O nucleo", "Uma lampada", "Uma onda de som"],
                    "correct_index": 0,
                    "explanation": "O nucleo fica no centro, com particulas ao redor.",
                    "visual_type": "atom",
                },
                {
                    "id": "moderna-2",
                    "question": "Por que essa parte da fisica parece invisivel?",
                    "options": ["Porque trata de coisas muito pequenas", "Porque nao existe", "Porque so fala de planetas"],
                    "correct_index": 0,
                    "explanation": "Ela estuda fenomenos pequenos demais para vermos diretamente.",
                    "visual_type": "atom",
                },
                {
                    "id": "moderna-3",
                    "question": "No desenho do atomo, as particulas ao redor lembram:",
                    "options": ["Uma regiao em volta do nucleo", "Uma estrada de carros", "Uma lente"],
                    "correct_index": 0,
                    "explanation": "O modelo visual ajuda a imaginar uma regiao ao redor do nucleo.",
                    "visual_type": "atom",
                },
            ],
        },
    }
    return missions.get(
        subject_id,
        {
            "stage_label": f"Fase de {subject_name}",
            "visual_type": "generic",
            "exercises": [
                {
                    "id": f"{subject_id}-1",
                    "question": "Qual e o primeiro passo para entender um problema?",
                    "options": ["Observar a situacao", "Chutar a resposta", "Pular a explicacao"],
                    "correct_index": 0,
                    "explanation": "Antes de calcular, observe o que esta acontecendo.",
                    "visual_type": "generic",
                }
            ],
        },
    )
