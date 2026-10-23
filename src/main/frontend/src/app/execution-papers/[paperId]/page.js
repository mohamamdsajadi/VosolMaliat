'use client';
import { useParams } from 'next/navigation';
import ExecutionPapersWorkspace from '../../components/execution-papers/execution-papers-workspace';

export default function ExecutionPaperDetailsPage() { const { paperId } = useParams(); return <ExecutionPapersWorkspace selectedPaperId={paperId} />; }
