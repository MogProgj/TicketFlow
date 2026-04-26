import { useState } from 'react'
import { ApiError, createComment } from '../api/client'
import type { TicketComment } from '../api/types'

interface Props {
    ticketId: number
    comments: TicketComment[]
    onCommentAdded: () => void
}

function formatDate(iso: string): string {
    return new Date(iso).toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    })
}

export default function CommentsPanel({ ticketId, comments, onCommentAdded }: Props) {
    const [author, setAuthor] = useState('')
    const [body, setBody] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setError(null)
        setSubmitting(true)
        try {
            await createComment(ticketId, { author: author.trim(), body: body.trim() })
            setBody('')
            onCommentAdded()
        } catch (err) {
            setError(err instanceof ApiError ? err.message : 'Failed to post comment.')
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <section className="panel comments-panel">
            <h3 className="panel-section-title">Comments <span className="count-badge">{comments.length}</span></h3>

            <div className="comments-list">
                {comments.length === 0 && <div className="state-message">No comments yet.</div>}
                {comments.map(c => (
                    <div key={c.id} className="comment-item">
                        <div className="comment-header">
                            <span className="comment-author">{c.author}</span>
                            <span className="comment-time">{formatDate(c.createdAt)}</span>
                        </div>
                        <div className="comment-body">{c.body}</div>
                    </div>
                ))}
            </div>

            <form className="comment-form" onSubmit={handleSubmit}>
                {error && <div className="form-error">{error}</div>}
                <input
                    className="input"
                    type="text"
                    placeholder="Your name"
                    value={author}
                    onChange={e => setAuthor(e.target.value)}
                    required
                    disabled={submitting}
                />
                <textarea
                    className="input textarea"
                    placeholder="Write a comment…"
                    value={body}
                    onChange={e => setBody(e.target.value)}
                    required
                    disabled={submitting}
                    rows={3}
                />
                <button className="btn btn-primary" type="submit" disabled={submitting}>
                    {submitting ? 'Posting…' : 'Post Comment'}
                </button>
            </form>
        </section>
    )
}
