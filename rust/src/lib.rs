//! Platform-independent task state machine. Device transport is implemented in a later phase.

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum TaskState {
    Pending,
    Running,
    Paused,
    Completed,
    Failed,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Task {
    pub id: String,
    pub state: TaskState,
}

impl Task {
    pub fn new(id: impl Into<String>) -> Result<Self, &'static str> {
        let id = id.into();
        if id.trim().is_empty() {
            return Err("task id cannot be empty");
        }
        Ok(Self { id, state: TaskState::Pending })
    }

    pub fn start(&mut self) -> Result<(), &'static str> {
        match self.state {
            TaskState::Pending | TaskState::Paused => {
                self.state = TaskState::Running;
                Ok(())
            }
            _ => Err("task is not startable"),
        }
    }

    pub fn pause(&mut self) -> Result<(), &'static str> {
        if self.state != TaskState::Running {
            return Err("only running tasks can be paused");
        }
        self.state = TaskState::Paused;
        Ok(())
    }

    pub fn complete(&mut self) -> Result<(), &'static str> {
        if self.state != TaskState::Running {
            return Err("only running tasks can be completed");
        }
        self.state = TaskState::Completed;
        Ok(())
    }

    pub fn fail(&mut self) -> Result<(), &'static str> {
        if self.state != TaskState::Running {
            return Err("only running tasks can fail");
        }
        self.state = TaskState::Failed;
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn task_can_start_pause_and_resume() {
        let mut task = Task::new("daily").unwrap();
        task.start().unwrap();
        task.pause().unwrap();
        task.start().unwrap();
        assert_eq!(task.state, TaskState::Running);
    }

    #[test]
    fn running_task_can_complete() {
        let mut task = Task::new("contest").unwrap();
        task.start().unwrap();
        task.complete().unwrap();
        assert_eq!(task.state, TaskState::Completed);
        assert_eq!(task.start(), Err("task is not startable"));
    }

    #[test]
    fn running_task_can_fail() {
        let mut task = Task::new("farm").unwrap();
        task.start().unwrap();
        task.fail().unwrap();
        assert_eq!(task.state, TaskState::Failed);
    }

    #[test]
    fn empty_task_id_is_rejected() {
        assert_eq!(Task::new("  "), Err("task id cannot be empty"));
    }

    #[test]
    fn pending_task_cannot_be_paused() {
        let mut task = Task::new("daily").unwrap();
        assert_eq!(task.pause(), Err("only running tasks can be paused"));
    }
}
